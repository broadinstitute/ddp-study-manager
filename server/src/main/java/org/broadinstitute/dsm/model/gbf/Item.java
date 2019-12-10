package org.broadinstitute.dsm.model.gbf;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

public class Item {

    private String itemNumber;
    private String lotNumber;
    private String serialNumber;
    private String expireDate;
    private String shippedQty;
    private List<SubItem> subItem;

    public Item() {
    }

    public Item(String itemNumber, String lotNumber, String serialNumber, String expireDate, String shippedQty, List<SubItem> subItem) {
        this.itemNumber = itemNumber;
        this.lotNumber = lotNumber;
        this.serialNumber = serialNumber;
        this.expireDate = expireDate;
        this.shippedQty = shippedQty;
        this.subItem = subItem;
    }

    @XmlAttribute(name="ItemNumber")
    public String getItemNumber() {
        return itemNumber;
    }

    public void setItemNumber(String itemNumber) {
        this.itemNumber = itemNumber;
    }

    @XmlAttribute(name="LotNumber")
    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    @XmlAttribute(name="SerialNumber")
    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    @XmlAttribute(name="ExpireDate")
    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    @XmlAttribute(name="ShippedQty")
    public String getShippedQty() {
        return shippedQty;
    }

    public void setShippedQty(String shippedQty) {
        this.shippedQty = shippedQty;
    }

    @XmlElement(name="SubItem")
    public List<SubItem> getSubItem() {
        return subItem;
    }

    public void setSubItem(List<SubItem> subItem) {
        this.subItem = subItem;
    }
}
