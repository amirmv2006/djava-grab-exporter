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
import java.text.SimpleDateFormat;
import java.util.*;
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
        int monthToReport = 0;
        if (args.length > 0) {
            monthToReport = Integer.valueOf(args[0]);
        } else {
            Calendar calendar = new GregorianCalendar();
            monthToReport = calendar.get(Calendar.MONTH);
            monthToReport = (monthToReport + 11) % 12;
        }
        System.out.println("monthToReport = " + monthToReport);
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
                Folder receiptFolder = store.getFolder(configuration.getString("grab.email.folder"));
                receiptFolder.open(Folder.READ_ONLY);
                Message[] messages = receiptFolder.getMessages();
                emails = new ArrayList<Email>();
                List<Message> messagesCopy = new ArrayList<Message>();
                messagesCopy.add(messages[0]);
                for (Message message : messages) {
                    Date receivedDate = message.getReceivedDate();
                    Calendar calendar = new GregorianCalendar();
                    calendar.setTime(receivedDate);
                    if (calendar.get(Calendar.MONTH) == monthToReport) {
                        Object content = message.getContent();
                        Email email = new Email();
                        email.setUber(false);
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
                }
                Folder uberReceiptFolder = store.getFolder(configuration.getString("uber.email.folder"));
                uberReceiptFolder.open(Folder.READ_ONLY);
                Message[] uberMessages = uberReceiptFolder.getMessages();
                List<Message> uberMessagesCopy = new ArrayList<Message>();
                uberMessagesCopy.add(uberMessages[0]);
                for (Message message : uberMessages) {
                    Date receivedDate = message.getReceivedDate();
                    Calendar calendar = new GregorianCalendar();
                    calendar.setTime(receivedDate);
                    if (calendar.get(Calendar.MONTH) == monthToReport) {
                        Object content = message.getContent();
                        Email email = new Email();
                        email.setUber(true);
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
                }
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(emailsFileName));
                objectOutputStream.writeObject(emails);
                objectOutputStream.flush();
                objectOutputStream.close();
            }
            System.out.println("Fetched the emails, generating report");
            System.out.println("emails.size() = " + emails.size());
            int successfulyFetchedSumCount = 0;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            List<Email> duplicates = new ArrayList<Email>();
            for (int i = 0; i < emails.size(); i++) {
                try {
                    Email email = emails.get(i);
                    Document doc = Jsoup.parse(email.getHtml());
                    if (email.isUber()) {
                        Elements dateElements = doc.getElementsByClass("date");
                        Calendar calendar = new GregorianCalendar();
                        for (Element dateElement : dateElements) {
                            String text = dateElement.text();
                            calendar.setTime(new SimpleDateFormat("MMMM dd, yyyy").parse(text));
                        }
                        Elements fromElements = doc.getElementsByClass("from");
                        for (Element fromElement : fromElements) {
                            Elements addressElements = fromElement.parent().getElementsByClass("address");
                            for (Element addressElement : addressElements) {
                                String text = addressElement.text();
                                email.setPickUp(text);
                            }
                            String text = fromElement.text();
                            Pattern compile = Pattern.compile("(\\d\\d)\\:(\\d\\d)(.*)");
                            Matcher matcher = compile.matcher(text);
                            if (matcher.find()) {
                                Integer h = Integer.valueOf(matcher.group(1));
                                Integer m = Integer.valueOf(matcher.group(2));
                                String amPm = matcher.group(3);
                                calendar.set(Calendar.HOUR, h);
                                calendar.set(Calendar.MINUTE, m);
                                calendar.set(Calendar.AM_PM, amPm.equalsIgnoreCase("am") ? Calendar.AM : Calendar.PM);
                            }
                        }
                        Elements toElements = doc.getElementsByClass("to");
                        for (Element toElement : toElements) {
                            Elements addressElements = toElement.parent().getElementsByClass("address");
                            for (Element addressElement : addressElements) {
                                String text = addressElement.text();
                                email.setDropOff(text);
                            }
                        }
                        email.setDate(calendar.getTime());
                        boolean duplicate = false;
                        for (Email prevEmails : emails) {
                            if (prevEmails != email && email.getDate().equals(prevEmails.getDate())) {
                                duplicate = true;
                                duplicates.add(email);
                                break;
                            }
                        }
                        if (duplicate) {
                            continue;
                        }
                        Elements elementsByClass = doc.getElementsByClass("final-charge");
                        for (Element element : elementsByClass) {
                            String spanText = element.text();
                            Pattern compile = Pattern.compile(".*MYR(\\d*[.]?\\d+)");
                            Matcher matcher = compile.matcher(spanText);
                            if (matcher.find()) {
                                Double amount = Double.valueOf(matcher.group(1));
                                email.setAmount(amount);
                                successfulyFetchedSumCount++;
                                System.out.println("Fetched Total: " + amount);
                            }
                        }
                    } else {
                        Elements spanElements = doc.getElementsByTag("span");
                        Iterator<Element> iterator = spanElements.iterator();
                        int addressCounter = 0;
                        while (iterator.hasNext()) {
                            Element nextSpan = iterator.next();
                            String spanText = nextSpan.text();
                            if (spanText.contains("+0800")) {
                                String dateTime = spanText.substring(0, spanText.length() - " +0800".length());
                                Date parsed = simpleDateFormat.parse(dateTime);
                                email.setDate(parsed);
                            } else if (spanText.toLowerCase().contains(",")) {
                                if (addressCounter == 0) {
                                    email.setPickUp(spanText);
                                    addressCounter++;
                                } else if (addressCounter == 1) {
                                    email.setDropOff(spanText);
                                    addressCounter++;
                                }
                            }
                        }
                        Elements elements = doc.getElementsByAttributeValueMatching("width", "44%");
                        ListIterator<Element> tdElemenetsIterator = elements.listIterator();
                        while (tdElemenetsIterator.hasNext()) {
                            Element td = tdElemenetsIterator.next();
                            Element spanElement = td.child(1);
                            String spanText = spanElement.text();
                            Pattern compile = Pattern.compile("[-+]?(\\d*[.])?\\d+");
                            Matcher matcher = compile.matcher(spanText);
                            if (matcher.find()) {
                                Double amount = Double.valueOf(matcher.group());
                                email.setAmount(amount);
                                successfulyFetchedSumCount++;
                                System.out.println("Fetched Total: " + amount);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("i = " + i);
                }
            }
            emails.removeAll(duplicates);
            Collections.sort(emails, new Comparator<Email>() {
                public int compare(Email o1, Email o2) {
                    return o1.getDate().compareTo(o2.getDate());
                }
            });
            System.out.println("successfulyFetchedSumCount = " + successfulyFetchedSumCount);
            ReportCreator.generateReport(emails, configuration, "email.jrxmlfile");
            ReportCreator.generateReport(emails, configuration, "clailsheet.jrxmlfile");
        } catch (Exception mex) {
            mex.printStackTrace();
        }
    }
}
