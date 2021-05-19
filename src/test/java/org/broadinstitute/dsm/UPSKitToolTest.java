package org.broadinstitute.dsm;

import org.broadinstitute.dsm.db.DDPInstance;
import org.broadinstitute.dsm.util.model.Kit;
import org.broadinstitute.dsm.util.tools.TbosUPSKitTool;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.broadinstitute.dsm.TestHelper.setupDB;

public class UPSKitToolTest {
    @Test
    public void testDateConversion(){
        String dateTimeStringInUTC = "2020-12-16T14:46:51Z";
        String dateTimeStringInEST = TbosUPSKitTool.getSQLDateTimeString(dateTimeStringInUTC);
        Assert.assertEquals(dateTimeStringInEST, "2020-12-16 09:46:51");
    }

    @Test
    public void testKitSetValue(){
        Kit kit = new Kit();
        kit.setValueByHeader("kit label", "aaa");
        Assert.assertEquals(kit.getKitLabel(), "aaa");
        kit.setValueByHeader("reason", null);
        Assert.assertEquals(kit.getReason(), null);
        kit.setValueByHeader("result", "result1");
        Assert.assertEquals(kit.getResult(), "result1");
        kit.setValueByHeader("requested at", "2020-09-18T13:54:14.159Z");
        Assert.assertEquals(kit.getRequestedAt(), "2020-09-18T13:54:14.159Z");
        kit.setValueByHeader("shipped at", "2020-09-18T11:54:14.159Z");
        Assert.assertEquals(kit.getShippedAt(), "2020-09-18T11:54:14.159Z");
        kit.setValueByHeader("delivered at", "2020-09-15T11:54:14.159Z");
        Assert.assertEquals(kit.getDeliveredAt(), "2020-09-15T11:54:14.159Z");
        kit.setValueByHeader("picked up at", "2020-08-15T11:54:14.159Z");
        Assert.assertEquals(kit.getPickedUpAt(), "2020-08-15T11:54:14.159Z");
        kit.setValueByHeader("received at", "2020-08-15T12:54:14.159Z");
        Assert.assertEquals(kit.getReceivedAt(), "2020-08-15T12:54:14.159Z");
        kit.setValueByHeader("resulted at", "2019-08-15T12:54:14.159Z");
        Assert.assertEquals(kit.getResultedAt(), "2019-08-15T12:54:14.159Z");
    }
    @Test
    public void testReadFile(){
//        String fileContent = TestUtil.readFile("NdiTestFile.txt");
        Map<String, ArrayList<Kit>> participants = TbosUPSKitTool.readFile("src/test/resources/TbosUPSKitToolTestFile.csv");
        Assert.assertNotNull(participants);
        Assert.assertEquals(participants.size(), 1);
        Assert.assertTrue(participants.keySet().contains("TestBoston_PMVYGB"));
        Assert.assertEquals(participants.get("TestBoston_PMVYGB").size(), 1);
        ArrayList<Kit> ptKits = participants.get("TestBoston_PMVYGB");
        Assert.assertEquals(ptKits.size(), 1);
        Kit kit = ptKits.get(0);
        Assert.assertEquals(kit.getKitLabel(), "TBOS-test");
        Assert.assertNull(kit.getReason());
        Assert.assertNull(kit.getResult());
    }

    @Test
    public void testReadFileMultiPt(){
        Map<String, ArrayList<Kit>> participants = TbosUPSKitTool.readFile("src/test/resources/TbosUPSKitToolMultiPtKits.csv");
        Assert.assertNotNull(participants);
        Assert.assertEquals(participants.size(), 2);
        Assert.assertTrue(participants.keySet().contains("TestBoston_PMVYGB"));
        Assert.assertTrue(participants.keySet().contains("TestBoston_PBPD68"));
        Assert.assertEquals(participants.get("TestBoston_PMVYGB").size(), 1);
        Assert.assertEquals(participants.get("TestBoston_PBPD68").size(), 2);
        ArrayList<Kit> ptKits = participants.get("TestBoston_PMVYGB");
        Assert.assertEquals(ptKits.size(), 1);
        Kit kit = ptKits.get(0);
        Assert.assertEquals(kit.getKitLabel(), "TBOS-test");
        Assert.assertNull(kit.getReason());
        Assert.assertNull(kit.getResult());
    }

    @Test
    public void testGetDDPKitBasedOnKitLabel(){
        DDPInstance ddpInstance = DDPInstance.getDDPInstance("testboston");
        Kit outboundKit = TbosUPSKitTool.getDDBKitBasedOnKitLabel(TbosUPSKitTool.SQL_SELECT_KIT_BY_KIT_LABEL +TbosUPSKitTool.SQL_SELECT_OUTBOUND, "TBOS-test", ddpInstance.getDdpInstanceId());
        Assert.assertNotNull(outboundKit);
        Map<String, ArrayList<Kit>> participants = TbosUPSKitTool.readFile("src/test/resources/TbosUPSKitToolTestFile.csv");
        Assert.assertNotNull(participants);
        Assert.assertEquals(participants.size(), 1);
        Assert.assertTrue(participants.keySet().contains("TestBoston_PMVYGB"));
        Assert.assertEquals(participants.get("TestBoston_PMVYGB").size(), 1);
        ArrayList<Kit> ptKits = participants.get("TestBoston_PMVYGB");
        Assert.assertEquals(ptKits.get(0).getGuid(), outboundKit.getGuid());
        Assert.assertEquals(ptKits.get(0).getShortId(), outboundKit.getShortId());
        Assert.assertEquals(ptKits.get(0).getKitLabel(), outboundKit.getKitLabel());
        Assert.assertEquals("1Z9YA775YW21328147", outboundKit.getTrackingToId());
        Assert.assertEquals("2309", outboundKit.getDsmKitRequestId());

        Kit inboundKit = TbosUPSKitTool.getDDBKitBasedOnKitLabel(TbosUPSKitTool.SQL_SELECT_KIT_BY_KIT_LABEL +TbosUPSKitTool.SQL_SELECT_INBOUND, "TBOS-test", ddpInstance.getDdpInstanceId());
        Assert.assertNotNull(inboundKit);
        Assert.assertTrue(participants.keySet().contains("TestBoston_PMVYGB"));
        Assert.assertEquals(participants.get("TestBoston_PMVYGB").size(), 1);
        Assert.assertEquals(ptKits.get(0).getGuid(), inboundKit.getGuid());
        Assert.assertEquals(ptKits.get(0).getShortId(), inboundKit.getShortId());
        Assert.assertEquals(ptKits.get(0).getKitLabel(), inboundKit.getKitLabel());
        Assert.assertEquals("92748902118533553033028108", inboundKit.getTrackingReturnId());
        Assert.assertEquals("2309", outboundKit.getDsmKitRequestId());
    }

//    @Test
//    public void





    @BeforeClass
    public static void first() {
        setupDB();
    }
//
//    @AfterClass
//    public static void stopServer() {
//        cleanupDB();
//    }


}
