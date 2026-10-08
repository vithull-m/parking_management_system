package com.parking.service;

import com.parking.dao.BookingStatusHistoryDao;
import com.parking.dao.ReservationDao;
import com.parking.dao.ResourceDao;
import com.parking.dao.UserDao;
import com.parking.db.DatabaseManager;
import com.parking.exception.ServiceException;
import com.parking.exception.UnauthorizedException;
import com.parking.exception.ValidationException;
import com.parking.model.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BookingService {
    private final DatabaseManager dbManager;
    private final ResourceDao resourceDao;
    private final ReservationDao reservationDao;
    private final BookingStatusHistoryDao historyDao;
    private final UserDao userDao;

    public BookingService(DatabaseManager dbManager,
                          ResourceDao resourceDao,
                          ReservationDao reservationDao,
                          BookingStatusHistoryDao historyDao,
                          UserDao userDao) {
        this.dbManager = dbManager;
        this.resourceDao = resourceDao;
        this.reservationDao = reservationDao;
        this.historyDao = historyDao;
        this.userDao = userDao;
    }

    /**
     * Calculate price breakdown based on current rate and requested quantity.
     */
    public FeeBreakdown calculateFee(double rate, int quantity) {
        validateQuantity(quantity);
        return new FeeBreakdown(rate, quantity);
    }

    /**
     * Confirm a new booking atomically.
     */
    public Reservation confirmBooking(BookingRequest request) {
        // 1. Validate inputs
        validateBookingRequest(request);

        // Verify requesting user exists
        User user = userDao.findById(request.getUserId())
                .orElseThrow(() -> new ValidationException("User not found for ID: " + request.getUserId()));

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 2. Recheck slot availability and active state
            Optional<Resource> optResource = resourceDao.findByIdForUpdate(conn, request.getResourceId());
            if (optResource.isEmpty()) {
                throw new ValidationException("Selected parking slot does not exist.");
            }
            Resource resource = optResource.get();
            if (!resource.isActive()) {
                throw new ValidationException("Parking slot " + resource.getIdentifier() + " is currently inactive.");
            }
            if (resource.getAvailableQuantity() < request.getQuantity()) {
                throw new ValidationException("Insufficient availability for slot " + resource.getIdentifier() +
                        ". Requested: " + request.getQuantity() + ", Available: " + resource.getAvailableQuantity());
            }

            // 3. Snapshot rate & calculate total
            double currentRate = resource.getRate();
            FeeBreakdown fee = new FeeBreakdown(currentRate, request.getQuantity());

            // 4. Generate unique booking reference
            String bookingRef = generateBookingReference();

            // 5. Build Reservation
            Reservation reservation = new Reservation();
            reservation.setBookingReference(bookingRef);
            reservation.setUserId(user.getId());
            reservation.setCustomerName(request.getCustomerName().trim());
            reservation.setCustomerPhone(request.getCustomerPhone().trim());
            reservation.setVehicleNumber(request.getVehicleNumber().trim().toUpperCase());
            reservation.setBookingDate(request.getBookingDate());
            reservation.setBaseAmount(fee.getBaseAmount());
            reservation.setBookingCharge(fee.getBookingCharge());
            reservation.setGrandTotal(fee.getGrandTotal());
            reservation.setStatus(BookingStatus.CONFIRMED);
            reservation.setCreatedAt(LocalDateTime.now());

            // 6. Build ReservationItem snapshot
            String category = (resource.getSchedule() != null) ? resource.getSchedule().getCategory() : "Parking Bay";
            ReservationItem item = new ReservationItem(
                    resource.getId(),
                    resource.getIdentifier(),
                    resource.getName(),
                    category,
                    currentRate,
                    request.getQuantity()
            );
            reservation.addItem(item);

            // 7. Insert Reservation & Items into DB
            reservation = reservationDao.insert(conn, reservation);

            // 8. Reduce availability atomically
            boolean reduced = resourceDao.reduceAvailability(conn, resource.getId(), request.getQuantity());
            if (!reduced) {
                throw new ValidationException("Failed to reserve slot capacity due to concurrent booking. Please retry.");
            }

            // 9. Record initial status history
            BookingStatusHistory history = new BookingStatusHistory(
                    reservation.getId(),
                    null,
                    BookingStatus.CONFIRMED,
                    user.getId(),
                    user.getRole(),
                    "Booking confirmed and slot reserved"
            );
            historyDao.insert(conn, history);
            reservation.addStatusHistory(history);

            // Commit atomic transaction
            conn.commit();
            return reservation;

        } catch (ValidationException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new ServiceException("Booking confirmation failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Cancel an eligible booking by the user.
     * Rule: User can cancel only CONFIRMED bookings.
     * Rule: Users can access/cancel only their own bookings.
     * Rule: Restores availability exactly once; repeated cancellation is rejected.
     */
    public Reservation cancelBooking(Long reservationId, Long requestingUserId) {
        if (reservationId == null || requestingUserId == null) {
            throw new ValidationException("Reservation ID and User ID are required for cancellation.");
        }

        User user = userDao.findById(requestingUserId)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + requestingUserId));

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            Optional<Reservation> optReservation = reservationDao.findByIdForUpdate(conn, reservationId);
            if (optReservation.isEmpty()) {
                throw new ValidationException("Reservation not found for ID: " + reservationId);
            }
            Reservation reservation = optReservation.get();

            // Authorization check: User can access/cancel only their own booking (Admins can also cancel if needed, but user permissions are strictly scoped)
            if (!user.isAdmin() && !reservation.getUserId().equals(requestingUserId)) {
                throw new UnauthorizedException("Access Denied: You cannot cancel another user's booking.");
            }

            // Check if already cancelled or in other non-cancellable state
            if (reservation.getStatus() == BookingStatus.CANCELLED) {
                throw new ValidationException("Booking " + reservation.getBookingReference() + " is already CANCELLED. Repeated cancellation is rejected.");
            }
            if (!reservation.getStatus().isCancellableByUser()) {
                throw new ValidationException("Cannot cancel booking in status '" + reservation.getStatus().getDisplayName() +
                        "'. Only CONFIRMED bookings can be cancelled.");
            }

            // Update status to CANCELLED
            reservationDao.updateStatus(conn, reservationId, BookingStatus.CANCELLED);

            // Restore availability for reserved resources (exactly once)
            for (ReservationItem item : reservation.getItems()) {
                resourceDao.restoreAvailability(conn, item.getResourceId(), item.getQuantity());
            }

            // Record status history
            BookingStatusHistory history = new BookingStatusHistory(
                    reservationId,
                    reservation.getStatus(),
                    BookingStatus.CANCELLED,
                    user.getId(),
                    user.getRole(),
                    "Cancelled by " + (user.isAdmin() ? "Admin" : "Customer") + "; capacity restored"
            );
            historyDao.insert(conn, history);

            conn.commit();

            reservation.setStatus(BookingStatus.CANCELLED);
            reservation.setStatusHistories(historyDao.findByReservationId(reservationId));
            return reservation;

        } catch (ValidationException | UnauthorizedException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new ServiceException("Booking cancellation failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Admin updates booking status strictly to the next valid status.
     * Flow: CONFIRMED -> CHECKED_IN -> COMPLETED (cannot skip stages).
     */
    public Reservation updateBookingStatusByAdmin(Long reservationId, Long adminUserId, BookingStatus targetStatus) {
        if (reservationId == null || adminUserId == null || targetStatus == null) {
            throw new ValidationException("Reservation ID, Admin User ID, and Target Status are required.");
        }

        User admin = userDao.findById(adminUserId)
                .orElseThrow(() -> new UnauthorizedException("Admin user not found: " + adminUserId));

        if (!admin.isAdmin()) {
            throw new UnauthorizedException("Access Denied: Only Administrators can update operational booking status.");
        }

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            Optional<Reservation> optReservation = reservationDao.findByIdForUpdate(conn, reservationId);
            if (optReservation.isEmpty()) {
                throw new ValidationException("Reservation not found for ID: " + reservationId);
            }
            Reservation reservation = optReservation.get();
            BookingStatus currentStatus = reservation.getStatus();

            // Validate transition rules
            if (currentStatus == BookingStatus.CANCELLED) {
                throw new ValidationException("Cannot update status of a CANCELLED booking.");
            }
            if (currentStatus == BookingStatus.COMPLETED) {
                throw new ValidationException("Booking is already COMPLETED. No further transitions allowed.");
            }
            if (!currentStatus.isValidAdminTransition(targetStatus)) {
                throw new ValidationException("Invalid status transition: Cannot move from " +
                        currentStatus.getDisplayName() + " to " + targetStatus.getDisplayName() +
                        ". Valid next status is: " + (currentStatus.getNextAdminStatus() != null ? currentStatus.getNextAdminStatus().getDisplayName() : "None") + ".");
            }

            // Update status
            reservationDao.updateStatus(conn, reservationId, targetStatus);

            // Record status history
            BookingStatusHistory history = new BookingStatusHistory(
                    reservationId,
                    currentStatus,
                    targetStatus,
                    admin.getId(),
                    admin.getRole(),
                    "Status advanced to " + targetStatus.getDisplayName() + " by Admin " + admin.getUsername()
            );
            historyDao.insert(conn, history);

            conn.commit();

            reservation.setStatus(targetStatus);
            reservation.setStatusHistories(historyDao.findByReservationId(reservationId));
            return reservation;

        } catch (ValidationException | UnauthorizedException e) {
            rollbackQuietly(conn);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(conn);
            throw new ServiceException("Status update failed: " + e.getMessage(), e);
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * View bookings for a specific user (users can access only their own bookings).
     */
    public List<Reservation> getUserBookings(Long requestingUserId, Long targetUserId) {
        User user = userDao.findById(requestingUserId)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + requestingUserId));

        if (!user.isAdmin() && !requestingUserId.equals(targetUserId)) {
            throw new UnauthorizedException("Access Denied: You cannot view another user's bookings.");
        }

        return reservationDao.findByUserId(targetUserId);
    }

    /**
     * View all bookings (Admin only).
     */
    public List<Reservation> getAllBookings(Long requestingUserId) {
        User user = userDao.findById(requestingUserId)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + requestingUserId));

        if (!user.isAdmin()) {
            throw new UnauthorizedException("Access Denied: Only Administrators can view all system bookings.");
        }

        return reservationDao.findAll();
    }

    /**
     * Find single reservation by ID with authorization check.
     */
    public Reservation getReservationById(Long reservationId, Long requestingUserId) {
        User user = userDao.findById(requestingUserId)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + requestingUserId));

        Reservation reservation = reservationDao.findById(reservationId)
                .orElseThrow(() -> new ValidationException("Reservation not found for ID: " + reservationId));

        if (!user.isAdmin() && !reservation.getUserId().equals(requestingUserId)) {
            throw new UnauthorizedException("Access Denied: You cannot view this booking.");
        }

        return reservation;
    }

    public List<BookingStatusHistory> getStatusHistory(Long reservationId, Long requestingUserId) {
        // Check access via getReservationById
        getReservationById(reservationId, requestingUserId);
        return historyDao.findByReservationId(reservationId);
    }

    // Input Validation Helpers
    public void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > 5) {
            throw new ValidationException("Quantity must be between 1 and 5 only (entered: " + quantity + ").");
        }
    }

    public void validateBookingRequest(BookingRequest req) {
        if (req == null) {
            throw new ValidationException("Booking request cannot be null.");
        }
        if (req.getUserId() == null) {
            throw new ValidationException("User ID is required.");
        }
        if (req.getResourceId() == null) {
            throw new ValidationException("Please select a parking slot.");
        }
        validateQuantity(req.getQuantity());

        if (req.getCustomerName() == null || req.getCustomerName().trim().isEmpty()) {
            throw new ValidationException("Customer name cannot be blank.");
        }

        if (req.getCustomerPhone() == null || !req.getCustomerPhone().trim().matches("^[0-9]{10}$")) {
            throw new ValidationException("Phone number must contain exactly 10 digits (digits only).");
        }

        if (req.getVehicleNumber() == null || req.getVehicleNumber().trim().isEmpty()) {
            throw new ValidationException("Vehicle number cannot be blank.");
        }

        if (req.getBookingDate() == null || req.getBookingDate().trim().isEmpty()) {
            throw new ValidationException("Booking date is required.");
        }

        try {
            LocalDate.parse(req.getBookingDate().trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid booking date format. Expected YYYY-MM-DD.");
        }
    }

    private String generateBookingReference() {
        String today = LocalDate.now().toString().replace("-", "");
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "PK-" + today + "-" + randomSuffix;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
            }
        }
    }

    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }
}
