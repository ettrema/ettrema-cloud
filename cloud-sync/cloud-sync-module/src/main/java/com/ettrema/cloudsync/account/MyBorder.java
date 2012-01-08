package com.ettrema.cloudsync.account;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import javax.swing.border.AbstractBorder;

/**
 *
 * @author brad
 */
public class MyBorder extends AbstractBorder {
    private static final long serialVersionUID = 1L;

    private int tabWidth;
    private int tabHeight;

    @Override
    public void paintBorder( Component c, Graphics g, int x, int y, int width, int height ) {
        float[] hsbvals = new float[3];
        Color.RGBtoHSB( 204, 204, 204, hsbvals );
        //Color lineColor = Color.getHSBColor( hsbvals[0], hsbvals[1], hsbvals[2]);
        Color lineColor = Color.RED;
        g.setColor( lineColor );
        g.drawRect( x, y, width - 1, height - 1 );
        g.drawRect( x + 2, y + 2, width - 5, height - 5 );
    }

    @Override
    public Insets getBorderInsets( Component c ) {
        return new Insets( 3, 3, 3, 3 );
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
