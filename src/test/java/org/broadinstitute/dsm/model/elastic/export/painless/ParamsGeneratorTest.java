package org.broadinstitute.dsm.model.elastic.export.painless;

import org.broadinstitute.dsm.db.KitRequestShipping;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class ParamsGeneratorTest {

    @Test
    public void generate() {
        KitRequestShipping kitRequestShipping = new KitRequestShipping(1L, 2L, "easyPostIdValue", "easyPostAddressIdValue", true, "msg");
        ParamsGenerator paramsGenerator = new ParamsGenerator(kitRequestShipping, "");
        Map<String, Object> paramsMap = paramsGenerator.generate();
        Map <String, Object> params = (Map <String, Object>) paramsMap.get("params");
        Map <String, Object> dsm = (Map <String, Object>) params.get("dsm");
        Map <String, Object> kitRequestShippingObj = (Map <String, Object>) dsm.get("kitRequestShipping");
        Assert.assertEquals(1L, kitRequestShippingObj.get("dsmKitRequestId"));
        Assert.assertEquals(2L, kitRequestShippingObj.get("dsmKitId"));
        Assert.assertEquals("easyPostIdValue", kitRequestShippingObj.get("easypostToId"));
        Assert.assertEquals(true, kitRequestShippingObj.get("error"));
        Assert.assertEquals("msg", kitRequestShippingObj.get("message"));
    }

}