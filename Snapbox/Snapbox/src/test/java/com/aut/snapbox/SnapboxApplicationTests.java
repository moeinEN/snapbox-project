package com.aut.snapbox;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aut.snapbox.Model.DeliveryInfo;
import com.aut.snapbox.Utils.Haversine;
import com.aut.snapbox.Utils.NormalDistanceCalc;
import com.aut.snapbox.Utils.ProcessUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class SnapboxApplicationTests {

    @Mock
    private DeliveryInfo deliveryInfo1;

    @Mock
    private DeliveryInfo deliveryInfo2;

    @InjectMocks
    private ProcessUtils processUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testHaversine() {
        double val = Math.toRadians(90);
        double expected = 0.4999999999999999;
        assertEquals(expected, Haversine.haversine(val), 0.001);
    }

    @Test
    void testCalculateDistance_Haversine() {
        double startLat = 51.5074;
        double startLong = -0.1278;
        double endLat = 48.8566;
        double endLong = 2.3522;
        double expected = 343.556;

        assertEquals(expected, Haversine.calculateDistance(startLat, startLong, endLat, endLong), 0.5);
    }

    @Test
    void testCalculateDistance_NormalDistanceCalc() {
        double lat1 = 51.5074;
        double lon1 = -0.1278;
        double lat2 = 48.8566;
        double lon2 = 2.3522;

        double[] expected = { 171.63888916088735, -294.75551154939586, 341.0875545386102 };

        double[] result = NormalDistanceCalc.calculateDistance(lat1, lon1, lat2, lon2);
        assertArrayEquals(expected, result, 0.5);
    }

    @Test
    void testRemoveInvalidEntries() {
        // Create mock DeliveryInfo objects
        DeliveryInfo deliveryInfo1 = mock(DeliveryInfo.class);
        DeliveryInfo deliveryInfo2 = mock(DeliveryInfo.class);

        when(deliveryInfo1.getLat()).thenReturn(51.5074);
        when(deliveryInfo1.getLng()).thenReturn(-0.1278);
        when(deliveryInfo1.getTimestamp()).thenReturn(1609459200L);

        when(deliveryInfo2.getLat()).thenReturn(48.8566);
        when(deliveryInfo2.getLng()).thenReturn(2.3522);
        when(deliveryInfo2.getTimestamp()).thenReturn(1609462800L);

        List<DeliveryInfo> deliveryInfos = new ArrayList<>();
        deliveryInfos.add(deliveryInfo1);
        deliveryInfos.add(deliveryInfo2);

        List<Double> result = ProcessUtils.removeInvalidEntries(deliveryInfos);
        assertEquals(0, result.size());
    }

    @Test
    void testCalculateFare() {
        List<DeliveryInfo> deliveryInfos = new ArrayList<>();
        deliveryInfos.add(deliveryInfo1);
        deliveryInfos.add(deliveryInfo2);

        List<Double> speeds = new ArrayList<>();
        speeds.add(50.0);

        double fare = ProcessUtils.calculateFare(deliveryInfos, speeds);
        assertEquals(3.47, fare, 0.5);
    }
}
