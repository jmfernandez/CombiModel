package es.csic.cnb.rmi.client.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXHyperlink;

import es.csic.cnb.curation.BeanCompound;

public class CompPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private JPanel busyPanel;

  /**
   * Create the panel.
   */
  public CompPanel() {
    setBackground(Color.WHITE);
    //setLayout(new MigLayout("", "[]", "[][][][]"));
    setLayout(new MigLayout("", "[][grow]"));

    busyPanel = createBusyPanel();
  }

  public void clean() {
    this.removeAll();

//    // Loading
//    add(busyPanel, "cell 1 0,grow");

    validate();
    repaint();
  }

  public void loader() {
    // Loading
    add(busyPanel, "cell 1 0,grow");

    validate();
    repaint();
  }

  private JPanel createBusyPanel() {
    JPanel busyPanel = new JPanel();
    busyPanel.setBackground(Color.WHITE);
    busyPanel.setLayout(new MigLayout("", "[grow,center]", "[grow,center]"));
    JLabel lbBusy = new JLabel("Loading...");
    lbBusy.setForeground(new Color(219, 75, 75));
    lbBusy.setIcon(new ImageIcon(this.getClass().getResource("/resources/busy.gif")));
    busyPanel.add(lbBusy, "cell 0 0");

    return busyPanel;
  }

  public void update(BeanCompound data) {
    this.removeAll();

    if (data.isExchangeCompound()) {
      JLabel lb = createTitleLabel("SBML ID [EX]:");
      lb.setIcon(new ImageIcon(this.getClass().getResource("/resources/alert.png")));
      lb.setToolTipText("Exchange compound");
      add(lb, "cell 0 1");
    }
    else {
      add(createTitleLabel("SBML ID:"), "cell 0 1");
    }
    add(createLabel(data.getSbmlId(), false), "cell 1 1");

    add(createTitleLabel("SBML NAME:"), "cell 0 2");
    add(createLabel(data.getSbmlName(), true), "cell 1 2");

    int nextCell = 3;
    Set<String> synList = data.getSynList();
    if (synList.size() <= 5) {
      for (String s : synList) {
        if (s.equals(data.getSbmlName())) continue;
        add(createTitleLabel("SBML SYN:"), "cell 0 " + nextCell);
        add(createLabel(s, true), "cell 1 " + nextCell);
        nextCell++;
      }
    }
    else {
      JXCollapsiblePane cp = new JXCollapsiblePane();
      cp.getContentPane().setBackground(Color.white);
      cp.setBackground(Color.white);
      cp.setCollapsed(true);
      cp.setLayout(new MigLayout("", "[]"));
      int nxCell = 0;
      for (String s : data.getSynList()) {
        if (s.equals(data.getSbmlName())) continue;
        cp.add(createLabel(s, true), "cell 0 " + nxCell);
        nxCell++;
      }
      // Show/hide the "Controls"
      JXHyperlink toggle = new JXHyperlink(cp.getActionMap().get(JXCollapsiblePane.TOGGLE_ACTION));
      toggle.setActionMap(cp.getActionMap());
      toggle.setText(nxCell + " SBML SYNs [show]:");
      toggle.setFont(new Font("Arial, Dialog", Font.BOLD, 11));
      toggle.setToolTipText("Show/Hide Syns Panel");
      add(toggle, "cell 0 " + nextCell);
      add(cp, "cell 1 " + nextCell);
      nextCell++;
    }

    for (String s : data.getChebiIdList()) {
      add(createTitleLabel("CHEBI ID:"), "cell 0 " + nextCell);
      add(new Link(s, getChebiURL(s)), "cell 1 " + nextCell);
      nextCell++;
    }
    for (String s : data.getKeggIdList()) {
      add(createTitleLabel("KEGG ID:"), "cell 0 " + nextCell);
      add(new Link(s, getKeggURL(s)), "cell 1 " + nextCell);
      nextCell++;
    }
    if (data.getSbmlFormula() != null) {
      add(createTitleLabel("SBML FORMULA:"), "cell 0 " + nextCell);
      add(createLabel(data.getSbmlFormula(), true), "cell 1 " + nextCell);
      nextCell++;
    }
    if (data.getInchi() != null) {
      add(createTitleLabel("INCHI:"), "cell 0 " + nextCell);
      add(createLabel(data.getInchi(), true), "cell 1 " + nextCell);
      nextCell++;
    }
    for (String sm : data.getSmilesList()) {
      add(createTitleLabel("SMILES:"), "cell 0 " + nextCell);
      add(createLabel(sm, true), "cell 1 " + nextCell);
      nextCell++;
    }
    add(createTitleLabel("CARGA:"), "cell 0 " + nextCell);
    add(createLabel(String.valueOf(data.getCharge())), "cell 1 " + nextCell);

    validate();
    repaint();
  }

  private JLabel createTitleLabel(String txt) {
    JLabel label = new JLabel(txt);
    label.setFont(new Font("Arial, Dialog", Font.BOLD, 11));
    return label;
  }

  private JLabel createLabel(String txt) {
    JLabel label = new JLabel(txt);
    label.setFont(new Font("Arial, Dialog", Font.PLAIN, 11));
    return label;
  }

  private JLabel createLabel(String txt, boolean chebi) {
    JLabel label = new JLabel(txt);
    label.setComponentPopupMenu(new LinksPopUpMenu(label, chebi));
    label.setFont(new Font("Arial, Dialog", Font.PLAIN, 11));
    return label;
  }

  private String getChebiURL(String query) {
    query = query.replaceAll("\\s", "+");
    query = query.replaceAll(":", "%3A");
    String url = "http://www.ebi.ac.uk/chebi/advancedSearchFT.do?searchString=";
    url += query;
    url += "&queryBean.stars=3&queryBean.stars=-1";

    return url;
  }

  private String getKeggURL(String query) {
    query = query.replaceAll("\\s", "+");
    query = query.replaceAll(":", "%3A");
    String url = "http://www.genome.jp/dbget-bin/www_bget?cpd:";
    url += query;

    return url;
  }
}
