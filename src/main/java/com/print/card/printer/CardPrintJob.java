package com.print.card.printer;


import javax.print.*;
import javax.print.DocFlavor.SERVICE_FORMATTED;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.PageRanges;
import java.awt.*;
import java.awt.image.ImageObserver;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

class CardPrintJob implements Printable {
    private String frontImagePath;
    private String backImagePath;

    public CardPrintJob(String frontImagePath, String backImagePath) {
        this.frontImagePath = frontImagePath;
        this.backImagePath = backImagePath;
    }

    public void print() {
        PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
        if (printService != null) {
            DocPrintJob printJob = printService.createPrintJob();
            PrintRequestAttributeSet printRequestAttributeSet = new HashPrintRequestAttributeSet();
            printRequestAttributeSet.add(new PageRanges(1, 2));
            Doc doc = new SimpleDoc(this, SERVICE_FORMATTED.PRINTABLE, (DocAttributeSet)null);

            try {
                printJob.print(doc, printRequestAttributeSet);
            } catch (PrintException var6) {
                var6.printStackTrace();
            }
        } else {
            System.out.println("没有找到打印机！");
        }

    }

    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex == 0) {
            this.drawImage(graphics, this.frontImagePath, pageFormat.getImageableX(), pageFormat.getImageableY());
            return 0;
        } else if (pageIndex == 1) {
            this.drawImage(graphics, this.backImagePath, pageFormat.getImageableX(), pageFormat.getImageableY());
            return 0;
        } else {
            return 1;
        }
    }

    private void drawImage(Graphics graphics, String imagePath, double x, double y) {
        Image image = Toolkit.getDefaultToolkit().getImage(imagePath);
        graphics.drawImage(image, (int)x, (int)y, (ImageObserver)null);
    }
}
