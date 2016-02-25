package es.csic.cnb.rmi.client.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

public class LinksPopUpMenu extends JPopupMenu implements ActionListener {
  private static final long serialVersionUID = -9120558535124649385L;

  private static final Color ALPHA_ZERO = new Color(0, true);
  private static final Color POPUP_BACK = new Color(250,250,250,230);
  private static final Color POPUP_LEFT = new Color(230,230,230,230);
  private static final int LEFT_WIDTH = 24;

  private JLabel label;

  public LinksPopUpMenu(JLabel label, boolean chebi) {
    super();

    this.label = label;

    setBorder(new LineBorder(Color.gray));

    JMenuItem itmCopy = new JMenuItem("Copy");
    itmCopy.setActionCommand("Copy");
    itmCopy.addActionListener(this);
    add(itmCopy);
    addSeparator();

    JMenuItem itmGoogle = new JMenuItem("Google");
    itmGoogle.setActionCommand("Google");
    itmGoogle.addActionListener(this);
    add(itmGoogle);

    if (chebi) {
      JMenuItem itmChebi = new JMenuItem("Chebi");
      itmChebi.setActionCommand("Chebi");
      itmChebi.addActionListener(this);
      add(itmChebi);
    }
  }

  @Override public boolean isOpaque() {
    return false;
  }

  @Override public JMenuItem add(JMenuItem menuItem) {
    menuItem.setOpaque(false);
    return super.add(menuItem);
  }

  @Override public void show(Component c, int x, int y) {
    EventQueue.invokeLater(new Runnable() {
      @Override public void run() {
        Window p = SwingUtilities.getWindowAncestor(LinksPopUpMenu.this);
        if(p!=null && p instanceof JWindow) {
          ((JWindow)p).setBackground(ALPHA_ZERO);
        }
      }
    });
    super.show(c, x, y);
  }

  @Override protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D)g.create();
    g2.setPaint(POPUP_LEFT);
    g2.fillRect(0,0,LEFT_WIDTH,getHeight());
    g2.setPaint(POPUP_BACK);
    g2.fillRect(LEFT_WIDTH,0,getWidth(),getHeight());
    g2.dispose();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if ("Copy".equals(e.getActionCommand())) {
      System.out.println(label.getText());

      StringSelection data = new StringSelection(label.getText());
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(data, data);
    }
    else if ("Google".equals(e.getActionCommand())) {
      browse(getGoogleURL(label.getText()));
    }
    else if ("Chebi".equals(e.getActionCommand())) {
      browse(getChebiURL(label.getText()));
    }
  }

  private String getChebiURL(String query) {
    query = query.replaceAll("\\s", "+");
    query = query.replaceAll(":", "%3A");
    String url = "http://www.ebi.ac.uk/chebi/advancedSearchFT.do?searchString=";
    url += query;
    url += "&queryBean.stars=3&queryBean.stars=-1";

    return url;
  }

  private String getGoogleURL(String query) {
    String url = "http://www.google.com/search?q=";
    url += query.replaceAll("\\s", "+");

    return url;
  }

  private void browse(String url) {
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

