package es.csic.cnb.rmi.client.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;

import javax.swing.Icon;
import javax.swing.JLabel;

public class Link extends JLabel implements MouseListener {
  private static final long serialVersionUID = 1L;
  
  private String url;

  public Link() {
    super();
    init("");
  }

  public Link(Icon image) {
    super(image);
    init("");
  }

  public Link(Icon image, int horizontalAlignment) {
    super(image, horizontalAlignment);
    init("");
  }

  public Link(String text) {
    super(text);
    init(text);
  }

  public Link(String text, Icon icon, int horizontalAlignment) {
    super(text, icon, horizontalAlignment);
    init(text);
  }
  
  public Link(String text, String url) {
    super(text);
    init(url);
  }
  
  public Link(String text, int horizontalAlignment) {
    super(text, horizontalAlignment);
    init(text);
  }

  public void setURL(String url) {
    this.url = url;
    this.setToolTipText("Open " + url + " in your browser");
  }

  private void init(String url) {
    setURL(url);
    this.addMouseListener(this);
    this.setForeground(Color.BLUE);
  }

  @Override
  public void mouseClicked(MouseEvent arg0) {
    browse();
  }

  @Override
  public void mouseEntered(MouseEvent arg0) {
    setCursor(new Cursor(Cursor.HAND_CURSOR));
  }

  @Override
  public void mouseExited(MouseEvent arg0) {
    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
  }

  @Override
  public void mousePressed(MouseEvent arg0) {
  }

  @Override
  public void mouseReleased(MouseEvent arg0) {
  }

  private void browse() {
    if (Desktop.isDesktopSupported()) {
      Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
          desktop.browse( new URI(url) );
        }
        catch (Exception e) {
          System.err.println(e.getMessage());
        }
      }
      else {
        System.err.println("Desktop doesn't support the browse action (fatal)");
      }
    }
    else {
      System.err.println("Desktop is not supported (fatal)");
    }
  }
}
