package ir.amv.snippets.djava.grab;

import ir.amv.snippets.djava.grab.mail.Email;
import ir.amv.snippets.djava.grab.report.ReportCreator;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.*;
import javax.mail.internet.MimeMultipart;

/**
 * Created by AMV on 4/26/2016.
 */
public class Runner {

    public static final String EMAILS_FILE_NAME = "emails.obj";

    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "imaps");
        try {
            ObjectInputStream objectInputStream = null;
            List<Email> emails = null;
            Configurations configurationsObj = new Configurations();
            PropertiesConfiguration configuration = configurationsObj.properties("config.properties");
            String emailsFileName = configuration.getString("email.file", EMAILS_FILE_NAME);
            try {
                objectInputStream = new ObjectInputStream(new FileInputStream(emailsFileName));
                emails = (List<Email>) objectInputStream.readObject();
            } catch (IOException e) {
            }
            if (emails == null || emails.isEmpty()) {
                Session session = Session.getInstance(props, null);
                Store store = session.getStore();
                store.connect("imap.gmail.com", configuration.getString("email.username"), configuration.getString("email.password"));
                Folder receiptFolder = store.getFolder(configuration.getString("email.folder"));
                receiptFolder.open(Folder.READ_ONLY);
                Message[] messages = receiptFolder.getMessages();
                emails = new ArrayList<Email>();
                List<Message> messagesCopy = new ArrayList<Message>();
                messagesCopy.add(messages[0]);
                for (Message message : messages) {
                    Object content = message.getContent();
                    Email email = new Email();
                    if (content instanceof MimeMultipart) {
                        email.setHtml("");
                        MimeMultipart mimeMultipart = (MimeMultipart) content;
                        int count = mimeMultipart.getCount();
                        for (int i = 0; i < count; i++) {
                            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                            Object bodyContent = bodyPart.getContent();
                            if (bodyPart.getContentType().toLowerCase().contains("html")) {
                                email.setHtml(email.getHtml() + bodyContent);
                            }
                        }
                    } else if (message.getContentType().toLowerCase().contains("html")) {
                        email.setHtml(message.getContent().toString());
                    }
                    emails.add(email);
                }
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(emailsFileName));
                objectOutputStream.writeObject(emails);
                objectOutputStream.flush();
                objectOutputStream.close();
            }
            System.out.println("Fetched the emails, generating report");
            for (int i = 0; i < emails.size(); i++) {
                try {
                    Email email = emails.get(i);
                    Document doc = Jsoup.parse(email.getHtml());
                    Elements elements = doc.getElementsByAttributeValueMatching("width", "44%");
                    ListIterator<Element> tdElemenetsIterator = elements.listIterator();
                    while (tdElemenetsIterator.hasNext()) {
                        Element td = tdElemenetsIterator.next();
                        Element spanElement = td.child(1);
                        String spanText = spanElement.text();
                        Pattern compile = Pattern.compile("[-+]?(\\d*[.])?\\d+");
                        Matcher matcher = compile.matcher(spanText);
                        if (matcher.find()) {
                            email.setAmount(Double.valueOf(matcher.group()));
                        }
                    }
//                    Elements img = doc.getElementsByTag("img");
//                    ListIterator<Element> elementListIterator1 = img.listIterator();
//                    while (elementListIterator1.hasNext()) {
//                        elementListIterator1.next().remove();
//                    }
//                    email.setHtml(doc.html());
                } catch (Exception e) {
                    System.out.println("i = " + i);
                }
            }
            ReportCreator.generateReport(emails, configuration);
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }
}
