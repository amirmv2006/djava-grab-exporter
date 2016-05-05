package ir.amv.snippets.djava.grab.report;

import ir.amv.snippets.djava.grab.mail.Email;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.xml.JRXmlLoader;
import org.apache.commons.configuration2.PropertiesConfiguration;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by AMV on 4/26/2016.
 */
public class ReportCreator {

    public static void generateReport(List<Email> emails, PropertiesConfiguration configuration, String reportFileConfigName) {
        try {
            JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(emails);

            Map reportParams = new HashMap();
            reportParams.put(JRParameter.IS_IGNORE_PAGINATION, Boolean.TRUE);

            JasperReport reportCompiled;
            FileInputStream is = new FileInputStream(configuration.getString(reportFileConfigName));
            JasperDesign reportDesign = JRXmlLoader.load(is);
            reportCompiled = JasperCompileManager.compileReport(reportDesign);
            JasperPrint reportPrinted = JasperFillManager.fillReport(reportCompiled, reportParams, ds);

            String format = configuration.getString(reportFileConfigName + ".outputformat", "docx");
            JRExporter exporter = getExporter(format);
            FileOutputStream fileOutputStream = new FileOutputStream(configuration.getString(reportFileConfigName + ".outputfilename") + "." + format);
            exporter.setParameter(JRExporterParameter.JASPER_PRINT,
                    reportPrinted);
            exporter.setParameter(JRExporterParameter.OUTPUT_STREAM,
                    fileOutputStream);
            exporter.exportReport();
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    private static JRExporter getExporter(String format) {
        if (format.equalsIgnoreCase("docx")) {
            return new JRDocxExporter();
        } else if (format.equalsIgnoreCase("pdf")) {
            return new JRPdfExporter();
        } else if (format.equalsIgnoreCase("docx")) {
            return new JRRtfExporter();
        } else if (format.equalsIgnoreCase("csv")) {
            return new JRCsvExporter();
        }
        return new JRHtmlExporter();
    }

}
