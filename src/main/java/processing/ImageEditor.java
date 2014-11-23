package processing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.io.File;

public class ImageEditor {

public static BufferedImage brightnessImage(BufferedImage image,int val){
val=(val*255)/100;
for (int x = 0; x < image.getWidth(); x++) {
    for (int y = 0; y < image.getHeight(); y++) 
        {
        Color color = new Color(image.getRGB(x, y));

        int r, g, b;
        r = checkColorRange(color.getRed() + val);
        g = checkColorRange(color.getGreen() + val);
        b = checkColorRange(color.getBlue() + val);

        color = new Color(r, g, b);
        image.setRGB(x, y, color.getRGB());
        }
    }
	image.flush();
	return image;
}
public static BufferedImage changeBrightness(BufferedImage src,float val){
	val=(val*255)/100;
    RescaleOp brighterOp = new RescaleOp(val, 0, null);
    return brighterOp.filter(src,null); //filtering
}
public static BufferedImage changeContrast(BufferedImage src,float val){
	val=(val*255)/100;
    RescaleOp brighterOp = new RescaleOp(1, val, null);
    return brighterOp.filter(src,null); //filtering
}
public static BufferedImage toSepia(BufferedImage img, int sepiaIntensity) {
	sepiaIntensity=(sepiaIntensity*255)/100;
    BufferedImage sepia = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
    // Play around with this.  20 works well and was recommended
    //   by another developer. 0 produces black/white image
    int sepiaDepth = 20;

    int w = img.getWidth();
    int h = img.getHeight();

    WritableRaster raster = sepia.getRaster();

    // We need 3 integers (for R,G,B color values) per pixel.
    int[] pixels = new int[w * h * 3];
    img.getRaster().getPixels(0, 0, w, h, pixels);

    for (int x = 0; x < img.getWidth(); x++) {
        for (int y = 0; y < img.getHeight(); y++) {

            int rgb = img.getRGB(x, y);
            Color color = new Color(rgb, true);
            int r = color.getRed();
            int g = color.getGreen();
            int b = color.getBlue();
            int gry = (r + g + b) / 3;

            r = g = b = gry;
            r = r + (sepiaDepth * 2);
            g = g + sepiaDepth;

            if (r > 255) {
                r = 255;
            }
            if (g > 255) {
                g = 255;
            }
            if (b > 255) {
                b = 255;
            }

            // Darken blue color to increase sepia effect
            b -= sepiaIntensity;

            // normalize if out of bounds
            if (b < 0) {
                b = 0;
            }
            if (b > 255) {
                b = 255;
            }

            color = new Color(r, g, b, color.getAlpha());
            sepia.setRGB(x, y, color.getRGB());

        }
    }

    return sepia;
}
public static BufferedImage negateImage(BufferedImage image){
	for (int x = 0; x < image.getWidth(); x++) {
	    for (int y = 0; y < image.getHeight(); y++) {
	        int rgba = image.getRGB(x, y);
	        Color col = new Color(rgba, true);
	        col = new Color(255 - col.getRed(),
	                        255 - col.getGreen(),
	                        255 - col.getBlue());
	        image.setRGB(x, y, col.getRGB());
	    }
	}
	image.flush();
	return image;
}






private static int checkColorRange(int newColor){
    if(newColor > 255){
        newColor = 255;
    } else if (newColor < 0) {
        newColor = 0;
    }
    return newColor;
}
}