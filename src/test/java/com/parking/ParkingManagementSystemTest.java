package com.parking;

import com.parking.dao.*;
import com.parking.db.DatabaseManager;
import com.parking.exception.UnauthorizedException;
import com.parking.exception.ValidationException;
import com.parking.model.*;
import com.parking.service.AuthService;
import com.parking.service.AvailabilityService;
import com.parking.service.BookingService;
import com.parking.service.ResourceManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class ParkingManagementSystemTest {

    private DatabaseManager dbManager;
    private UserDao userDao;
    private ScheduleDao scheduleDao;
    private ResourceDao resourceDao;
    private BookingStatusHistoryDao historyDao;
    private ReservationDao reservationDao;
    private AvailabilityService availabilityService;
    private BookingService bookingService;
    private ResourceManagementService resourceManagementService;

    private static final String TEST_DB = "jdbc:sqlite:target/test_parking.db";

    @BeforeEach
    void setUp() {
        // Ensure test directory exists
        new File("target").mkdirs();
        dbManager = new DatabaseManager(TEST_DB);
        dbManager.resetDatabase();

        userDao = new UserDao(dbManager);
        scheduleDao = new ScheduleDao(dbManager);
        resourceDao = new ResourceDao(dbManager);
        historyDao = new BookingStatusHistoryDao(dbManager);
        reservationDao = new ReservationDao(dbManager, historyDao);

        availabilityService = new AvailabilityService(resourceDao);
        bookingService = new BookingService(dbManager, resourceDao, reservationDao, historyDao, userDao);
        resourceManagementService = new ResourceManagementService(resourceDao, scheduleDao, userDao);
    }

    @Test
    @DisplayName("1. Quantity Boundaries: 1 and 5 are accepted; 0, negative, and >5 are rejected")
    void testQuantityBoundaries() {
        // Valid quantities: 1 to 5
        assertDoesNotThrow(() -> bookingService.validateQuantity(1));
        assertDoesNotThrow(() -> bookingService.validateQuantity(3));
        assertDoesNotThrow(() -> bookingService.validateQuantity(5));

        // Invalid quantities: 0, negative, >5
        ValidationException ex0 = assertThrows(ValidationException.class, () -> bookingService.validateQuantity(0));
        assertTrue(ex0.getMessage().contains("between 1 and 5"));

        ValidationException exNeg = assertThrows(ValidationException.class, () -> bookingService.validateQuantity(-1));
        assertTrue(exNeg.getMessage().contains("between 1 and 5"));

        ValidationException ex6 = assertThrows(ValidationException.class, () -> bookingService.validateQuantity(6));
        assertTrue(ex6.getMessage().contains("between 1 and 5"));

        ValidationException ex100 = assertThrows(ValidationException.class, () -> bookingService.validateQuantity(100));
        assertTrue(ex100.getMessage().contains("between 1 and 5"));
    }

    @Test
    @DisplayName("2. Unavailable Slot: R104 has 0 available capacity and cannot be booked")
    void testUnavailableSlot() {
        Resource r104 = resourceDao.findByIdentifier("R104").orElseThrow();
        assertEquals(0, r104.getAvailableQuantity(), "R104 should have 0 available spots");

        User user1 = userDao.findByUsername("user1").orElseThrow();

        BookingRequest req = new BookingRequest(
                user1.getId(),
                r104.getId(),
                1,
                "John Doe",
                "9876543210",
                "MH-12-AB-1234",
                LocalDate.now().toString()
        );

        ValidationException ex = assertThrows(ValidationException.class, () -> bookingService.confirmBooking(req));
        assertTrue(ex.getMessage().contains("Insufficient availability"), "Error should state insufficient availability");

        // Verify no booking was created
        List<Reservation> userBookings = reservationDao.findByUserId(user1.getId());
        assertEquals(0, userBookings.size(), "No booking should be created on failure");
    }

    @Test
    @DisplayName("3. Fare Boundaries: ₹999 -> ₹50 fee, ₹1000 -> ₹0 fee, ₹1001 -> ₹0 fee")
    void testFareBoundaries() {
        // Rule: ₹50 if base < ₹1000, otherwise ₹0.

        // Case 1: Base Amount = ₹999.00 (< 1000)
        FeeBreakdown fee999 = bookingService.calculateFee(999.00, 1);
        assertEquals(999.00, fee999.getBaseAmount(), 0.001);
        assertEquals(50.00, fee999.getBookingCharge(), 0.001, "Base < 1000 must incur ₹50 booking charge");
        assertEquals(1049.00, fee999.getGrandTotal(), 0.001);

        // Case 2: Base Amount = ₹1000.00 (>= 1000)
        FeeBreakdown fee1000 = bookingService.calculateFee(1000.00, 1);
        assertEquals(1000.00, fee1000.getBaseAmount(), 0.001);
        assertEquals(0.00, fee1000.getBookingCharge(), 0.001, "Base >= 1000 must have ₹0 booking charge");
        assertEquals(1000.00, fee1000.getGrandTotal(), 0.001);

        // Case 3: Base Amount = ₹1001.00 (>= 1000)
        FeeBreakdown fee1001 = bookingService.calculateFee(1001.00, 1);
        assertEquals(1001.00, fee1001.getBaseAmount(), 0.001);
        assertEquals(0.00, fee1001.getBookingCharge(), 0.001, "Base >= 1000 must have ₹0 booking charge");
        assertEquals(1001.00, fee1001.getGrandTotal(), 0.001);

        // Case 4: Base Amount with quantity: R101 (₹499) x 2 = ₹998 (< 1000) -> ₹50 charge -> ₹1048 total
        FeeBreakdown fee499x2 = bookingService.calculateFee(499.00, 2);
        assertEquals(998.00, fee499x2.getBaseAmount(), 0.001);
        assertEquals(50.00, fee499x2.getBookingCharge(), 0.001);
        assertEquals(1048.00, fee499x2.getGrandTotal(), 0.001);

        // Case 5: Base Amount with quantity: R101 (₹499) x 3 = ₹1497 (>= 1000) -> ₹0 charge -> ₹1497 total
        FeeBreakdown fee499x3 = bookingService.calculateFee(499.00, 3);
        assertEquals(1497.00, fee499x3.getBaseAmount(), 0.001);
        assertEquals(0.00, fee499x3.getBookingCharge(), 0.001);
        assertEquals(1497.00, fee499x3.getGrandTotal(), 0.001);
    }

    @Test
    @DisplayName("4. Successful Booking: creates CONFIRMED reservation, saves snapshot, reduces availability, records history")
    void testSuccessfulBooking() {
        Resource r101 = resourceDao.findByIdentifier("R101").orElseThrow();
        int initialAvail = r101.getAvailableQuantity();
        assertEquals(10, initialAvail);

        User user1 = userDao.findByUsername("user1").orElseThrow();

        BookingRequest req = new BookingRequest(
                user1.getId(),
                r101.getId(),
                2,
                "John Doe",
                "9876543210",
                "DL-01-XY-9999",
                LocalDate.now().toString()
        );

        Reservation res = bookingService.confirmBooking(req);

        // Assertions on returned reservation
        assertNotNull(res.getId());
        assertNotNull(res.getBookingReference());
        assertTrue(res.getBookingReference().startsWith("PK-"));
        assertEquals(BookingStatus.CONFIRMED, res.getStatus());
        assertEquals(998.00, res.getBaseAmount(), 0.001);
        assertEquals(50.00, res.getBookingCharge(), 0.001);
        assertEquals(1048.00, res.getGrandTotal(), 0.001);
        assertEquals("DL-01-XY-9999", res.getVehicleNumber());

        // Snapshot assertion
        assertEquals(1, res.getItems().size());
        ReservationItem item = res.getItems().get(0);
        assertEquals("R101", item.getResourceIdentifierSnapshot());
        assertEquals("Parking Slot A", item.getResourceNameSnapshot());
        assertEquals(499.00, item.getRateSnapshot(), 0.001);
        assertEquals(2, item.getQuantity());
        assertEquals(998.00, item.getSubtotal(), 0.001);

        // Availability reduced assertion
        Resource updatedR101 = resourceDao.findByIdentifier("R101").orElseThrow();
        assertEquals(initialAvail - 2, updatedR101.getAvailableQuantity());

        // History recorded assertion
        List<BookingStatusHistory> history = historyDao.findByReservationId(res.getId());
        assertEquals(1, history.size());
        assertEquals(BookingStatus.CONFIRMED, history.get(0).getToStatus());
        assertNull(history.get(0).getFromStatus());
    }

    @Test
    @DisplayName("5. Invalid Details: empty name, invalid phone digits, invalid date format are rejected")
    void testInvalidDetails() {
        Resource r101 = resourceDao.findByIdentifier("R101").orElseThrow();
        User user1 = userDao.findByUsername("user1").orElseThrow();

        // 5a. Empty Name
        BookingRequest emptyName = new BookingRequest(
                user1.getId(), r101.getId(), 1, "   ", "9876543210", "KA-01-1234", "2026-10-08");
        ValidationException exName = assertThrows(ValidationException.class, () -> bookingService.confirmBooking(emptyName));
        assertTrue(exName.getMessage().contains("Customer name cannot be blank"));

        // 5b. Phone with 9 digits
        BookingRequest shortPhone = new BookingRequest(
                user1.getId(), r101.getId(), 1, "John", "987654321", "KA-01-1234", "2026-10-08");
        ValidationException exPhone1 = assertThrows(ValidationException.class, () -> bookingService.confirmBooking(shortPhone));
        assertTrue(exPhone1.getMessage().contains("exactly 10 digits"));

        // 5c. Phone with letters
        BookingRequest alphaPhone = new BookingRequest(
                user1.getId(), r101.getId(), 1, "John", "987654321A", "KA-01-1234", "2026-10-08");
        ValidationException exPhone2 = assertThrows(ValidationException.class, () -> bookingService.confirmBooking(alphaPhone));
        assertTrue(exPhone2.getMessage().contains("exactly 10 digits"));

        // 5d. Empty Vehicle Number
        BookingRequest emptyVehicle = new BookingRequest(
                user1.getId(), r101.getId(), 1, "John", "9876543210", "  ", "2026-10-08");
        ValidationException exVeh = assertThrows(ValidationException.class, () -> bookingService.confirmBooking(emptyVehicle));
        assertTrue(exVeh.getMessage().contains("Vehicle number cannot be blank"));

        // 5e. Invalid Date format
        BookingRequest badDate = new BookingRequest(
                user1.getId(), r101.getId(), 1, "John", "9876543210", "KA-01-1234", "08/10/2026");
        ValidationException exDate = assertThrows(ValidationException.class, () -> bookingService.confirmBooking(badDate));
        assertTrue(exDate.getMessage().contains("Invalid booking date format"));
    }

    @Test
    @DisplayName("6. Availability Change before Confirmation: Rejects booking if capacity falls below requested")
    void testAvailabilityChangeBeforeConfirmation() {
        Resource r105 = resourceDao.findByIdentifier("R105").orElseThrow();
        assertEquals(3, r105.getAvailableQuantity());

        User user1 = userDao.findByUsername("user1").orElseThrow();
        User user2 = userDao.findByUsername("user2").orElseThrow();

        // User 1 books 2 slots of R105 (available drops from 3 to 1)
        BookingRequest req1 = new BookingRequest(
                user1.getId(), r105.getId(), 2, "John Doe", "9876543210", "DL-01-A", "2026-10-08");
        bookingService.confirmBooking(req1);

        Resource updatedR105 = resourceDao.findById(r105.getId()).orElseThrow();
        assertEquals(1, updatedR105.getAvailableQuantity());

        // User 2 now attempts to book 2 slots of R105 (only 1 available)
        BookingRequest req2 = new BookingRequest(
                user2.getId(), r105.getId(), 2, "Jane Smith", "9123456780", "MH-02-B", "2026-10-08");
        ValidationException ex = assertThrows(ValidationException.class, () -> bookingService.confirmBooking(req2));
        assertTrue(ex.getMessage().contains("Insufficient availability"));

        // Verify remaining is still 1
        Resource finalR105 = resourceDao.findById(r105.getId()).orElseThrow();
        assertEquals(1, finalR105.getAvailableQuantity());
    }

    @Test
    @DisplayName("7. Access Denial: User cannot view/cancel another user's booking; non-admin cannot update status")
    void testAccessDenial() {
        User user1 = userDao.findByUsername("user1").orElseThrow();
        User user2 = userDao.findByUsername("user2").orElseThrow();
        Resource r101 = resourceDao.findByIdentifier("R101").orElseThrow();

        // User 1 creates a booking
        BookingRequest req = new BookingRequest(
                user1.getId(), r101.getId(), 1, "John Doe", "9876543210", "DL-01-A", "2026-10-08");
        Reservation res = bookingService.confirmBooking(req);

        // User 2 tries to view User 1's booking
        UnauthorizedException exView = assertThrows(UnauthorizedException.class,
                () -> bookingService.getReservationById(res.getId(), user2.getId()));
        assertTrue(exView.getMessage().contains("Access Denied"));

        // User 2 tries to cancel User 1's booking
        UnauthorizedException exCancel = assertThrows(UnauthorizedException.class,
                () -> bookingService.cancelBooking(res.getId(), user2.getId()));
        assertTrue(exCancel.getMessage().contains("Access Denied"));

        // User 1 (non-admin) tries to advance status
        UnauthorizedException exAdmin = assertThrows(UnauthorizedException.class,
                () -> bookingService.updateBookingStatusByAdmin(res.getId(), user1.getId(), BookingStatus.CHECKED_IN));
        assertTrue(exAdmin.getMessage().contains("Access Denied"));
    }

    @Test
    @DisplayName("8. Status Transitions: CONFIRMED -> CHECKED_IN -> COMPLETED; skipping stages is rejected")
    void testStatusTransitions() {
        User user1 = userDao.findByUsername("user1").orElseThrow();
        User admin = userDao.findByUsername("admin").orElseThrow();
        Resource r101 = resourceDao.findByIdentifier("R101").orElseThrow();

        BookingRequest req = new BookingRequest(
                user1.getId(), r101.getId(), 1, "John Doe", "9876543210", "DL-01-A", "2026-10-08");
        Reservation res = bookingService.confirmBooking(req);
        assertEquals(BookingStatus.CONFIRMED, res.getStatus());

        // Admin tries to skip CHECKED_IN and go directly to COMPLETED -> must fail
        ValidationException exSkip = assertThrows(ValidationException.class,
                () -> bookingService.updateBookingStatusByAdmin(res.getId(), admin.getId(), BookingStatus.COMPLETED));
        assertTrue(exSkip.getMessage().contains("Invalid status transition"));

        // Valid Step 1: CONFIRMED -> CHECKED_IN
        Reservation checkedInRes = bookingService.updateBookingStatusByAdmin(res.getId(), admin.getId(), BookingStatus.CHECKED_IN);
        assertEquals(BookingStatus.CHECKED_IN, checkedInRes.getStatus());

        // Valid Step 2: CHECKED_IN -> COMPLETED
        Reservation completedRes = bookingService.updateBookingStatusByAdmin(res.getId(), admin.getId(), BookingStatus.COMPLETED);
        assertEquals(BookingStatus.COMPLETED, completedRes.getStatus());

        // History check: 3 entries (CONFIRMED, CHECKED_IN, COMPLETED)
        List<BookingStatusHistory> histories = historyDao.findByReservationId(res.getId());
        assertEquals(3, histories.size());
        assertEquals(BookingStatus.CONFIRMED, histories.get(0).getToStatus());
        assertEquals(BookingStatus.CHECKED_IN, histories.get(1).getToStatus());
        assertEquals(BookingStatus.COMPLETED, histories.get(2).getToStatus());

        // Admin cannot transition from COMPLETED
        ValidationException exCompleted = assertThrows(ValidationException.class,
                () -> bookingService.updateBookingStatusByAdmin(res.getId(), admin.getId(), BookingStatus.CHECKED_IN));
        assertTrue(exCompleted.getMessage().contains("already COMPLETED"));
    }

    @Test
    @DisplayName("9. Cancellation Twice: Rejects second cancellation and restores availability exactly once")
    void testCancellationTwice() {
        Resource r102 = resourceDao.findByIdentifier("R102").orElseThrow();
        int initialAvail = r102.getAvailableQuantity();
        assertEquals(5, initialAvail);

        User user1 = userDao.findByUsername("user1").orElseThrow();

        BookingRequest req = new BookingRequest(
                user1.getId(), r102.getId(), 2, "John Doe", "9876543210", "DL-01-A", "2026-10-08");
        Reservation res = bookingService.confirmBooking(req);

        // Check availability reduced to 3
        Resource bookedR102 = resourceDao.findById(r102.getId()).orElseThrow();
        assertEquals(3, bookedR102.getAvailableQuantity());

        // First cancellation -> succeeds
        Reservation cancelledRes = bookingService.cancelBooking(res.getId(), user1.getId());
        assertEquals(BookingStatus.CANCELLED, cancelledRes.getStatus());

        // Availability restored to 5
        Resource restoredR102 = resourceDao.findById(r102.getId()).orElseThrow();
        assertEquals(5, restoredR102.getAvailableQuantity());

        // Second cancellation attempt -> must be rejected
        ValidationException ex2 = assertThrows(ValidationException.class,
                () -> bookingService.cancelBooking(res.getId(), user1.getId()));
        assertTrue(ex2.getMessage().contains("already CANCELLED"));

        // Availability must remain 5, not 7!
        Resource unchangedR102 = resourceDao.findById(r102.getId()).orElseThrow();
        assertEquals(5, unchangedR102.getAvailableQuantity());
    }

    @Test
    @DisplayName("10. Restart Persistence: Data persists across new DatabaseManager instance")
    void testRestartPersistence() {
        User user1 = userDao.findByUsername("user1").orElseThrow();
        Resource r103 = resourceDao.findByIdentifier("R103").orElseThrow();

        BookingRequest req = new BookingRequest(
                user1.getId(), r103.getId(), 3, "John Doe", "9876543210", "DL-01-A", "2026-10-08");
        Reservation res = bookingService.confirmBooking(req);
        String savedRef = res.getBookingReference();

        // Simulate application restart with new DatabaseManager on same DB file
        DatabaseManager newDbManager = new DatabaseManager(TEST_DB);
        ReservationDao newReservationDao = new ReservationDao(newDbManager, new BookingStatusHistoryDao(newDbManager));
        ResourceDao newResourceDao = new ResourceDao(newDbManager);

        Optional<Reservation> persistedRes = newReservationDao.findByReference(savedRef);
        assertTrue(persistedRes.isPresent(), "Booking must exist after DB restart");
        assertEquals("John Doe", persistedRes.get().getCustomerName());
        assertEquals(3, persistedRes.get().getTotalQuantity());
        assertEquals(BookingStatus.CONFIRMED, persistedRes.get().getStatus());

        Resource persistedR103 = newResourceDao.findByIdentifier("R103").orElseThrow();
        assertEquals(17, persistedR103.getAvailableQuantity(), "Availability (20 - 3 = 17) must persist across restart");
    }

    @Test
    @DisplayName("11. Rate-Change Snapshot: Updating slot master rate does not affect existing booking snapshots")
    void testRateChangeSnapshot() {
        User user1 = userDao.findByUsername("user1").orElseThrow();
        Resource r101 = resourceDao.findByIdentifier("R101").orElseThrow();
        assertEquals(499.00, r101.getRate(), 0.001);

        // Create booking at ₹499
        BookingRequest req = new BookingRequest(
                user1.getId(), r101.getId(), 1, "John Doe", "9876543210", "DL-01-A", "2026-10-08");
        Reservation res = bookingService.confirmBooking(req);
        assertEquals(499.00, res.getBaseAmount(), 0.001);
        assertEquals(549.00, res.getGrandTotal(), 0.001); // 499 + 50 = 549

        // Change master slot rate to ₹899.00 in the catalog
        resourceDao.updateRate(r101.getId(), 899.00);
        Resource updatedMasterSlot = resourceDao.findById(r101.getId()).orElseThrow();
        assertEquals(899.00, updatedMasterSlot.getRate(), 0.001);

        // Fetch the existing booking: snapshot must still be ₹499.00 and grand total ₹549.00
        Reservation retrievedRes = reservationDao.findById(res.getId()).orElseThrow();
        assertEquals(499.00, retrievedRes.getBaseAmount(), 0.001);
        assertEquals(549.00, retrievedRes.getGrandTotal(), 0.001);
        assertEquals(499.00, retrievedRes.getItems().get(0).getRateSnapshot(), 0.001);
    }

    @Test
    @DisplayName("12. User Registration: New users can register, duplicate usernames rejected, phone validated, login works")
    void testUserRegistration() {
        AuthService authService = new AuthService(userDao);

        // 12a. Successful Registration
        User registered = authService.register("robert99", "pass123", "Robert Williams", "9876501234");
        assertNotNull(registered.getId());
        assertEquals("robert99", registered.getUsername());
        assertEquals("Robert Williams", registered.getFullName());
        assertEquals(UserRole.USER, registered.getRole());
        assertEquals("9876501234", registered.getPhone());

        // 12b. Verify login works with registered credentials
        User loggedIn = authService.login("robert99", "pass123");
        assertEquals(registered.getId(), loggedIn.getId());

        // 12c. Reject duplicate username
        ValidationException exDup = assertThrows(ValidationException.class,
                () -> authService.register("robert99", "pass456", "Another Robert", "9876500000"));
        assertTrue(exDup.getMessage().contains("already taken"));

        // 12d. Reject invalid phone
        ValidationException exPhone = assertThrows(ValidationException.class,
                () -> authService.register("alice", "pass123", "Alice", "123"));
        assertTrue(exPhone.getMessage().contains("exactly 10 digits"));

        // 12e. Reject short password
        ValidationException exPass = assertThrows(ValidationException.class,
                () -> authService.register("bob", "12", "Bob", "9876543210"));
        assertTrue(exPass.getMessage().contains("at least 4 characters"));
    }

    @Test
    @DisplayName("Admin can add and update parking spots while preserving occupied capacity")
    void testAdminCanManageParkingSpots() {
        User admin = userDao.findByUsername("admin").orElseThrow();
        Resource spot = new Resource("R106", "Parking Slot F", 1L, 650.00, 8, 8, true);

        Resource added = resourceManagementService.addSpot(admin.getId(), spot);
        assertNotNull(added.getId());
        assertEquals(8, added.getAvailableQuantity());

        User user = userDao.findByUsername("user1").orElseThrow();
        bookingService.confirmBooking(new BookingRequest(
                user.getId(), added.getId(), 2, "John Doe", "9876543210", "DL-01-A", "2026-10-08"));

        Resource changes = new Resource("R106", "Updated Parking Slot F", 2L, 750.00, 10, 0, false);
        Resource updated = resourceManagementService.updateSpot(admin.getId(), added.getId(), changes);
        assertEquals("Updated Parking Slot F", updated.getName());
        assertEquals(2L, updated.getScheduleId());
        assertEquals(750.00, updated.getRate(), 0.001);
        assertEquals(10, updated.getTotalCapacity());
        assertEquals(8, updated.getAvailableQuantity());
        assertFalse(updated.isActive());
    }

    @Test
    @DisplayName("Parking spot management rejects non-admins and capacity below current occupancy")
    void testSpotManagementAuthorizationAndCapacityValidation() {
        User admin = userDao.findByUsername("admin").orElseThrow();
        User user = userDao.findByUsername("user1").orElseThrow();
        Resource original = resourceDao.findByIdentifier("R101").orElseThrow();

        Resource updates = new Resource("R101", "Parking Slot A", 1L, 499.00, 10, 10, true);
        assertThrows(UnauthorizedException.class,
                () -> resourceManagementService.updateSpot(user.getId(), original.getId(), updates));

        bookingService.confirmBooking(new BookingRequest(
                user.getId(), original.getId(), 2, "John Doe", "9876543210", "DL-01-A", "2026-10-08"));
        Resource tooSmall = new Resource("R101", "Parking Slot A", 1L, 499.00, 1, 1, true);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> resourceManagementService.updateSpot(admin.getId(), original.getId(), tooSmall));
        assertTrue(exception.getMessage().contains("occupied spots"));
        assertEquals(8, resourceDao.findById(original.getId()).orElseThrow().getAvailableQuantity());
    }
}
