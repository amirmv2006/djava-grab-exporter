package ir.amv.snippets.djava.grab.mail;

import java.io.Serializable;

/**
 * Created by AMV on 4/26/2016.
 */
public class Email implements Serializable {

    private String html;
    private Double amount;

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
}
