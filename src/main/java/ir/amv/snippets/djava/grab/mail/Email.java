package ir.amv.snippets.djava.grab.mail;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by AMV on 4/26/2016.
 */
public class Email implements Serializable {

    private String html;
    private Double amount;
    private Date date;
    private String pickUp;
    private String dropOff;

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getPickUp() {
        return pickUp;
    }

    public void setPickUp(String pickUp) {
        this.pickUp = pickUp;
    }

    public String getDropOff() {
        return dropOff;
    }

    public void setDropOff(String dropOff) {
        this.dropOff = dropOff;
    }
}
