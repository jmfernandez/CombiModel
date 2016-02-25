package es.csic.cnb.rmi.client.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;
import es.csic.cnb.curation.BeanCandidate;
import es.csic.cnb.curation.BeanCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.data.WSCompound.WebService;
import es.csic.cnb.util.Util;

public class OptPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private final static Logger LOGGER = Logger.getLogger(OptPanel.class.getName());

  public static final int IDX_NONE = -1;
  public static final int IDX_NULL = -2;

  private Matcher mtAlphaNum = Pattern.compile("[^A-Za-z0-9]").matcher("");

  private WSCompound wsCandidateSelected;
  private WSCompound dbCandidateSelected;

  private int idxWsSelected = IDX_NULL;
  private int idxDbSelected = IDX_NULL;

  private JPanel panelWsSelected;
  private JPanel panelDbSelected;
  private JPanel busyWsPanel;
  private JPanel busyDbPanel;

  private JPanel optWsPanel;
  private JPanel optDbPanel;

  private JScrollPane spWs;
  private JScrollPane spDb;

  /**
   * Create the panel.
   */
  public OptPanel() {
    setBorder(null);
    setLayout(new MigLayout("hidemode 2,wrap", "[grow,fill][grow,fill]", "[grow,fill][]"));

    optWsPanel = new JPanel();
    optWsPanel.setLayout(new MigLayout("", "[grow]", "[][][grow][grow]"));
    spWs = new JScrollPane(optWsPanel);
    spWs.getVerticalScrollBar().setUnitIncrement(16);
    spWs.setPreferredSize(new Dimension(1600, 1200));
    spWs.setBorder(new TitledBorder(null, "Select Chebi Candidate ", TitledBorder.LEADING, TitledBorder.TOP, null, null));
    add(spWs, "cell 0 0,alignx center,growy,hidemode 2,wrap");

    optDbPanel = new JPanel();
    optDbPanel.setLayout(new MigLayout("", "[grow]", "[][][grow][grow]"));
    spDb = new JScrollPane(optDbPanel);
    spDb.getVerticalScrollBar().setUnitIncrement(16);
    spDb.setPreferredSize(new Dimension(1600, 1200));
    spDb.setBorder(new TitledBorder(null, "Select Db Candidate ", TitledBorder.LEADING, TitledBorder.TOP, null, null));
    add(spDb, "cell 1 0,alignx center,growy,hidemode 2,wrap");

    busyWsPanel = createBusyPanel();
    busyDbPanel = createBusyPanel();
  }

  public void clean() {
    optWsPanel.removeAll();
    optDbPanel.removeAll();

    validate();
    repaint();
  }

  public void loader() {
    // Loading
    optWsPanel.add(busyWsPanel, "cell 0 0,grow");
    optDbPanel.add(busyDbPanel, "cell 0 0,grow");

    validate();
    repaint();
  }

  private JPanel createBusyPanel() {
    JPanel busyPanel = new JPanel();
    busyPanel.setLayout(new MigLayout("", "[grow,center]", "[grow,center]"));
    JLabel lbBusy = new JLabel("Loading...");
    lbBusy.setForeground(new Color(219, 75, 75));
    lbBusy.setIcon(new ImageIcon(this.getClass().getResource("/resources/busy.gif")));
    busyPanel.add(lbBusy, "cell 0 0");

    return busyPanel;
  }

  public void update(BeanCompound beanComp) {
    optWsPanel.removeAll();
    optDbPanel.removeAll();

    idxDbSelected = IDX_NONE;
    dbCandidateSelected = null;

    idxWsSelected = IDX_NONE;
    wsCandidateSelected = null;

    if (LOGGER.isLoggable(Level.FINEST)) {
      for (BeanCandidate candidate : beanComp.getBeanCandidateList()) {
        LOGGER.log(Level.FINEST, "-Candidate: {0}", candidate);
      }
    }

    if (beanComp.existsWsCandidates()) {
      List<BeanCandidate> candidatelist = beanComp.getBeanCandidateList();
      if (candidatelist.size() == 1) {
        BeanCandidate candidate = candidatelist.get(0);
        if (candidate.isWsCurated()) {
          // Save
          wsCandidateSelected = candidate.getWsCandidate();

          spWs.setVisible(false);
          spDb.setVisible(true);
          updateDbPanel(beanComp, candidate.getWsCandidate());
        }
        else {
          spWs.setVisible(true);
          spDb.setVisible(false);
          updateWsPanel(beanComp);
        }
      }
      else {
        spWs.setVisible(true);
        spDb.setVisible(false);
        updateWsPanel(beanComp);
      }
    }
    else {
      spWs.setVisible(false);
      spDb.setVisible(true);
      updateDbPanel(beanComp, null);
    }
  }

  /**
   * Actualizar DB PANEL
   * @param beanComp
   */
  private void updateDbInfoPanel(BeanCompound beanComp, WSCompound dbCandidate) {
    optDbPanel.removeAll();

    Color colorws = new Color(33, 97, 11);

    final JPanel panel = new JPanel();
    //panel.setBackground(new Color(248,248,255));
    //panel.setBackground(new Color(250,240,230));

    optDbPanel.add(panel, "cell 0 0,grow");
    panel.setLayout(new MigLayout("", "[][]", "[][]"));

    String sc = "DB id: " + dbCandidate.getId();
    dbCandidate.setMatchFormula(true);
    dbCandidate.setMatchName(true);
    dbCandidate.setMatchSyn(true);

    JLabel rdbtnComp = new JLabel(sc);
    rdbtnComp.setForeground(colorws);
    rdbtnComp.setFont(new Font("Arial, Dialog", Font.BOLD, 12));
    panel.add(rdbtnComp, "cell 0 0");

    JLabel labeln1 = createTitleLabel("NAME:");
    JLabel labeln2 = createLabel(dbCandidate.getName(), true);
    panel.add(labeln1, "cell 0 1");
    panel.add(labeln2, "cell 1 1");

    if (dbCandidate.isMatchName()) {
      String wscompName = dbCandidate.getName().toLowerCase();
      String normalWName = Util.getChemicalName(Util.splitChemicalName(wscompName));

      Set<String> compSynList = beanComp.getSynList();
      for (String compSyn : compSynList) {
        compSyn = compSyn.toLowerCase();
        String cleanSyn = mtAlphaNum.reset(compSyn).replaceAll("");

        if (cleanSyn.equals(mtAlphaNum.reset(wscompName).replaceAll(""))) {
          labeln1.setForeground(Color.red);
          labeln2.setForeground(Color.red);
        }
        else {
          String normalSyn = Util.getChemicalName(Util.splitChemicalName(compSyn));
          if (normalSyn.equals(normalWName)) {
            labeln1.setForeground(Color.red);
            labeln2.setForeground(Color.red);
          }
        }
      }
    }

    String sbmlId = dbCandidate.getXrefList().get(Util.SOURCE_SBML);
    if (sbmlId != null) {
      JLabel label1 = createTitleLabel("SBML: ");
      JLabel label2 = createLabel(sbmlId, true);
      panel.add(label1, "cell 0 2");
      panel.add(label2, "cell 1 2");
    }

    int nextCell = 3;

    Set<String> compSynList = beanComp.getSynList(); // incluye nombre
    Set<String> wscompSynList = dbCandidate.getSynList();

    for (String wscompSyn : wscompSynList) {
      JLabel label1 = createTitleLabel("SYN: ");
      JLabel label2 = createLabel(wscompSyn, true);
      panel.add(label1, "cell 0 " + nextCell);
      panel.add(label2, "cell 1 " + nextCell);
      nextCell++;

      wscompSyn = wscompSyn.toLowerCase();
      if (dbCandidate.isMatchName()) {
        String normalWName = Util.getChemicalName(Util.splitChemicalName(wscompSyn));
        for (String compSyn : compSynList) {
          compSyn = compSyn.toLowerCase();
          String cleanSyn = mtAlphaNum.reset(compSyn).replaceAll("");
          if (cleanSyn.equals(mtAlphaNum.reset(wscompSyn).replaceAll(""))) {
            label1.setForeground(Color.red);
            label2.setForeground(Color.red);
          }
          else {
            String normalSyn = Util.getChemicalName(Util.splitChemicalName(compSyn));
            if (normalSyn.equals(normalWName)) {
              label1.setForeground(Color.red);
              label2.setForeground(Color.red);
            }
          }
        }
      }
    }

    if (dbCandidate.getChebiId() != null) {
      JLabel label1c = createTitleLabel("CHEBI: ");
      Link label2c = new Link(dbCandidate.getChebiId(), getChebiURL(dbCandidate.getChebiId()));
      panel.add(label1c, "cell 0 " + nextCell);
      panel.add(label2c, "cell 1 " + nextCell);
      if (dbCandidate.isMatchChebiId()) {
        label1c.setForeground(Color.red);
        label2c.setForeground(Color.red);
      }
      nextCell++;
    }

    if (dbCandidate.getKeggId() != null) {
      JLabel label1k = createTitleLabel("KEGG: ");
      Link label2k = new Link(dbCandidate.getKeggId(), getKeggURL(dbCandidate.getKeggId()));
      panel.add(label1k, "cell 0 " + nextCell);
      panel.add(label2k, "cell 1 " + nextCell);
      if (dbCandidate.isMatchKeggId()) {
        label1k.setForeground(Color.red);
        label2k.setForeground(Color.red);
      }
      nextCell++;
    }

    // Inchi
    if (dbCandidate.getInchi() != null) {
      JLabel label1 = createTitleLabel("INCHI: ");
      JLabel label2 = createLabel(dbCandidate.getInchi(), false);
      panel.add(label1, "cell 0 " + nextCell);
      panel.add(label2, "cell 1 " + nextCell);
      nextCell++;
      if (dbCandidate.isMatchInchi()) {
        label1.setForeground(Color.red);
        label2.setForeground(Color.red);
      }
    }
    if (dbCandidate.getInchiKey() != null) {
      JLabel label1 = createTitleLabel("INCHIKEY: ");
      JLabel label2 = createLabel(dbCandidate.getInchiKey(), false);
      panel.add(label1, "cell 0 " + nextCell);
      panel.add(label2, "cell 1 " + nextCell);
      nextCell++;
    }

    // Smiles
    if (dbCandidate.getSmiles() != null) {
      JLabel label1 = createTitleLabel("SMILES: ");
      JLabel label2 = createLabel(dbCandidate.getSmiles(), false);
      panel.add(label1, "cell 0 " + nextCell);
      panel.add(label2, "cell 1 " + nextCell);
      nextCell++;
      if (dbCandidate.isMatchSmiles()) {
        label1.setForeground(Color.red);
        label2.setForeground(Color.red);
      }
    }

    // Formula
    Set<String> fmList = dbCandidate.getFormulaList().get(Util.SOURCE_FORMULA_SBML);
    if (fmList != null) {
      for (String fm : fmList) {
        JLabel label2 = createLabel(fm, true);
        JLabel label1 = createTitleLabel("FORMULA: ");
        if (dbCandidate.isMatchFormula() && fm.equals(beanComp.getSbmlFormula())) {
          label1.setForeground(Color.red);
          label2.setForeground(Color.red);
        }
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;
      }
    }

    // Carga
    JLabel label1 = createTitleLabel("CARGA: ");
    JLabel label2 = createLabel(String.valueOf(dbCandidate.getCharge()));
    panel.add(label1, "cell 0 " + nextCell);
    panel.add(label2, "cell 1 " + nextCell);
    nextCell++;
    if (beanComp.getCharge() != 0 && beanComp.getCharge() == dbCandidate.getCharge()) {
      label1.setForeground(Color.red);
      label2.setForeground(Color.red);
    }

    validate();
    repaint();
  }

  /**
   * Actualizar DB PANEL
   * @param beanComp
   */
  private void updateDbPanel(BeanCompound beanComp, WSCompound wsCandidate) {
    optDbPanel.removeAll();

    final List<WSCompound> dblist = beanComp.getDbCandidateList(wsCandidate);
    WebService ws = dblist.get(0).getWs();

    Color colorws = Color.blue;
    if (ws.equals(WSCompound.WebService.DB)) {
      colorws = new Color(33, 97, 11);
    }

    ButtonGroup group = new ButtonGroup();

    JPanel panelInf = new JPanel();
    FlowLayout flowLayout = (FlowLayout) panelInf.getLayout();
    flowLayout.setVgap(0);
    flowLayout.setAlignment(FlowLayout.LEFT);
    optDbPanel.add(panelInf, "cell 0 0,grow");

    JLabel lbNumCandidates = new JLabel("Nº candidates from " + ws + ": " + String.valueOf(dblist.size()));
    lbNumCandidates.setFont(new Font("Dialog", Font.BOLD, 10));
    panelInf.add(lbNumCandidates);


    int nextPanel = 2;

    // NONE ::::::::::::::::::::::::::::::
    final JPanel panel1 = new JPanel();
    if (nextPanel%2 != 0) {
      panel1.setBackground(new Color(248,248,255));
    }
    else {
      panel1.setBackground(new Color(250,240,230));
    }
    optDbPanel.add(panel1, "cell 0 " + nextPanel +",grow");
    panel1.setLayout(new MigLayout("", "[][]", "[][]"));

    JRadioButton rdbtnNone = new JRadioButton("NONE");
    rdbtnNone.setOpaque(false);
    rdbtnNone.setForeground(colorws);
    rdbtnNone.setFont(new Font("Arial, Dialog", Font.BOLD, 12));
    rdbtnNone.setActionCommand("NONE");
    rdbtnNone.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        executeAction(e, panel1, dblist);
      }
    });
    rdbtnNone.setSelected(true);
    panel1.setBorder(new LineBorder(Color.red));
    panelDbSelected = panel1;
    panel1.add(rdbtnNone, "cell 0 0");
    group.add(rdbtnNone);

    int idx = 0;
    // COMPOUNDS ::::::::::::::::::::::::::::::
    for (final WSCompound dbCandidate : dblist) {
      nextPanel++;

      final JPanel panel = new JPanel();
      if (nextPanel%2 != 0) {
        panel.setBackground(new Color(248,248,255));
      }
      else {
        panel.setBackground(new Color(250,240,230));
      }
      optDbPanel.add(panel, "cell 0 " + nextPanel +",grow");
      panel.setLayout(new MigLayout("", "[][]", "[][]"));

      String sc = dbCandidate.getChebiId();
      if (ws.equals(WSCompound.WebService.DB)) {
        sc = "DB id: " + dbCandidate.getId();
        dbCandidate.setMatchFormula(true);
        dbCandidate.setMatchName(true);
        dbCandidate.setMatchSyn(true);
      }

      JRadioButton rdbtnComp = new JRadioButton(sc);
      rdbtnComp.setOpaque(false);
      rdbtnComp.setForeground(colorws);
      rdbtnComp.setFont(new Font("Arial, Dialog", Font.BOLD, 12));
      rdbtnComp.setActionCommand(String.valueOf(idx));
      rdbtnComp.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          executeAction(e, panel, dblist);
        }
      });
      panel.add(rdbtnComp, "cell 0 0");
      group.add(rdbtnComp);

      if (ws.equals(WSCompound.WebService.CHEBI)) {
        rdbtnComp.setToolTipText("[Nº Matches: " + dbCandidate.getNumMatches()+"]: "+
                "ML:"+dbCandidate.getMinLevenshtein()+" - L:"+dbCandidate.getLevenshtein());
      }

      JLabel labeln1 = createTitleLabel("NAME:");
      JLabel labeln2 = createLabel(dbCandidate.getName(), true);
      panel.add(labeln1, "cell 0 1");
      panel.add(labeln2, "cell 1 1");

      if (dbCandidate.isMatchName()) {
        String wscompName = dbCandidate.getName().toLowerCase();
        String normalWName = Util.getChemicalName(Util.splitChemicalName(wscompName));

        Set<String> compSynList = beanComp.getSynList();
        for (String compSyn : compSynList) {
          compSyn = compSyn.toLowerCase();
          String cleanSyn = mtAlphaNum.reset(compSyn).replaceAll("");

          if (cleanSyn.equals(mtAlphaNum.reset(wscompName).replaceAll(""))) {
            labeln1.setForeground(Color.red);
            labeln2.setForeground(Color.red);
          }
          else {
            String normalSyn = Util.getChemicalName(Util.splitChemicalName(compSyn));
            if (normalSyn.equals(normalWName)) {
              labeln1.setForeground(Color.red);
              labeln2.setForeground(Color.red);
            }
          }
        }
      }

      String sbmlId = dbCandidate.getXrefList().get(Util.SOURCE_SBML);
      if (sbmlId != null) {
        JLabel label1 = createTitleLabel("SBML: ");
        JLabel label2 = createLabel(sbmlId, true);
        panel.add(label1, "cell 0 2");
        panel.add(label2, "cell 1 2");
      }

      int nextCell = 3;

      Set<String> compSynList = beanComp.getSynList(); // incluye nombre
      Set<String> wscompSynList = dbCandidate.getSynList();

      for (String wscompSyn : wscompSynList) {
        JLabel label1 = createTitleLabel("SYN: ");
        JLabel label2 = createLabel(wscompSyn, true);
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;

        wscompSyn = wscompSyn.toLowerCase();
        if (dbCandidate.isMatchName()) {
          String normalWName = Util.getChemicalName(Util.splitChemicalName(wscompSyn));
          for (String compSyn : compSynList) {
            compSyn = compSyn.toLowerCase();
            String cleanSyn = mtAlphaNum.reset(compSyn).replaceAll("");
            if (cleanSyn.equals(mtAlphaNum.reset(wscompSyn).replaceAll(""))) {
              label1.setForeground(Color.red);
              label2.setForeground(Color.red);
            }
            else {
              String normalSyn = Util.getChemicalName(Util.splitChemicalName(compSyn));
              if (normalSyn.equals(normalWName)) {
                label1.setForeground(Color.red);
                label2.setForeground(Color.red);
              }
            }
          }
        }
      }

      if (dbCandidate.getChebiId() != null) {
        JLabel label1c = createTitleLabel("CHEBI: ");
        Link label2c = new Link(dbCandidate.getChebiId(), getChebiURL(dbCandidate.getChebiId()));
        panel.add(label1c, "cell 0 " + nextCell);
        panel.add(label2c, "cell 1 " + nextCell);
        if (dbCandidate.isMatchChebiId()) {
          label1c.setForeground(Color.red);
          label2c.setForeground(Color.red);
        }
        nextCell++;
      }

      if (dbCandidate.getKeggId() != null) {
        JLabel label1k = createTitleLabel("KEGG: ");
        Link label2k = new Link(dbCandidate.getKeggId(), getKeggURL(dbCandidate.getKeggId()));
        panel.add(label1k, "cell 0 " + nextCell);
        panel.add(label2k, "cell 1 " + nextCell);
        if (dbCandidate.isMatchKeggId()) {
          label1k.setForeground(Color.red);
          label2k.setForeground(Color.red);
        }
        nextCell++;
      }

      // Inchi
      if (dbCandidate.getInchi() != null) {
        JLabel label1 = createTitleLabel("INCHI: ");
        JLabel label2 = createLabel(dbCandidate.getInchi(), false);
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;
        if (dbCandidate.isMatchInchi()) {
          label1.setForeground(Color.red);
          label2.setForeground(Color.red);
        }
      }
      if (dbCandidate.getInchiKey() != null) {
        JLabel label1 = createTitleLabel("INCHIKEY: ");
        JLabel label2 = createLabel(dbCandidate.getInchiKey(), false);
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;
      }

      // Smiles
      if (dbCandidate.getSmiles() != null) {
        JLabel label1 = createTitleLabel("SMILES: ");
        JLabel label2 = createLabel(dbCandidate.getSmiles(), false);
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;
        if (dbCandidate.isMatchSmiles()) {
          label1.setForeground(Color.red);
          label2.setForeground(Color.red);
        }
      }

      // Formula
      Set<String> fmList = dbCandidate.getFormulaList().get(Util.SOURCE_FORMULA_SBML);
      if (fmList != null) {
        for (String fm : fmList) {
          JLabel label2 = createLabel(fm, true);
          JLabel label1 = createTitleLabel("FORMULA: ");
          if (dbCandidate.isMatchFormula() && fm.equals(beanComp.getSbmlFormula())) {
            label1.setForeground(Color.red);
            label2.setForeground(Color.red);
          }
          panel.add(label1, "cell 0 " + nextCell);
          panel.add(label2, "cell 1 " + nextCell);
          nextCell++;
        }
      }

      // Carga
      JLabel label1 = createTitleLabel("CARGA: ");
      JLabel label2 = createLabel(String.valueOf(dbCandidate.getCharge()));
      panel.add(label1, "cell 0 " + nextCell);
      panel.add(label2, "cell 1 " + nextCell);
      nextCell++;
      if (beanComp.getCharge() != 0 && beanComp.getCharge() == dbCandidate.getCharge()) {
        label1.setForeground(Color.red);
        label2.setForeground(Color.red);
      }

      idx++;
    } // Fin for dbCandidate

    validate();
    repaint();
  }


  /**
   * Actualizar WS PANEL
   * @param beanComp
   */
  private void updateWsPanel(final BeanCompound beanComp) {
    optWsPanel.removeAll();

    final List<WSCompound> wslist = beanComp.getWSCandidateList();
    WebService ws = wslist.get(0).getWs();

    Color colorws = Color.blue;
    if (ws.equals(WSCompound.WebService.DB)) {
      colorws = new Color(33, 97, 11);
    }

    ButtonGroup group = new ButtonGroup();

    JPanel panelInf = new JPanel();
    FlowLayout flowLayout = (FlowLayout) panelInf.getLayout();
    flowLayout.setVgap(0);
    flowLayout.setAlignment(FlowLayout.LEFT);
    optWsPanel.add(panelInf, "cell 0 0,grow");

    JLabel lbNumCandidates = new JLabel("Nº candidates from " + ws + ": " + String.valueOf(wslist.size()));
    lbNumCandidates.setFont(new Font("Dialog", Font.BOLD, 10));
    panelInf.add(lbNumCandidates);


    int nextPanel = 2;

    // NONE ::::::::::::::::::::::::::::::
    final JPanel panel1 = new JPanel();
    if (nextPanel%2 != 0) {
      panel1.setBackground(new Color(248,248,255));
    }
    else {
      panel1.setBackground(new Color(250,240,230));
    }
    optWsPanel.add(panel1, "cell 0 " + nextPanel +",grow");
    panel1.setLayout(new MigLayout("", "[][]", "[][]"));

    JRadioButton rdbtnNone = new JRadioButton("NONE");
    rdbtnNone.setOpaque(false);
    rdbtnNone.setForeground(colorws);
    rdbtnNone.setFont(new Font("Arial, Dialog", Font.BOLD, 12));
    rdbtnNone.setActionCommand("NONE");
    rdbtnNone.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        executeAction(e, panel1, wslist, beanComp, null);
      }
    });
    rdbtnNone.setSelected(true);
    panel1.setBorder(new LineBorder(Color.red));
    panelWsSelected = panel1;
    panel1.add(rdbtnNone, "cell 0 0");
    group.add(rdbtnNone);

    int idx = 0;
    // COMPOUNDS ::::::::::::::::::::::::::::::
    for (final WSCompound wsCandidate : wslist) {
      nextPanel++;

      final JPanel panel = new JPanel();
      if (nextPanel%2 != 0) {
        panel.setBackground(new Color(248,248,255));
      }
      else {
        panel.setBackground(new Color(250,240,230));
      }
      optWsPanel.add(panel, "cell 0 " + nextPanel +",grow");
      panel.setLayout(new MigLayout("", "[][]", "[][]"));

      String sc = wsCandidate.getChebiId();
      if (ws.equals(WSCompound.WebService.DB)) {
        sc = "DB id: " + wsCandidate.getId();
        wsCandidate.setMatchFormula(true);
        wsCandidate.setMatchName(true);
        wsCandidate.setMatchSyn(true);
      }

      JRadioButton rdbtnComp = new JRadioButton(sc);
      rdbtnComp.setOpaque(false);
      rdbtnComp.setForeground(colorws);
      rdbtnComp.setFont(new Font("Arial, Dialog", Font.BOLD, 12));
      rdbtnComp.setActionCommand(String.valueOf(idx));
      rdbtnComp.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          executeAction(e, panel, wslist, beanComp, wsCandidate);
        }
      });
      panel.add(rdbtnComp, "cell 0 0");
      group.add(rdbtnComp);

      if (ws.equals(WSCompound.WebService.CHEBI)) {
        rdbtnComp.setToolTipText("[Nº Matches: " + wsCandidate.getNumMatches()+"]: "+
                "ML:"+wsCandidate.getMinLevenshtein()+" - L:"+wsCandidate.getLevenshtein());
      }

      JLabel labeln1 = createTitleLabel("NAME:");
      JLabel labeln2 = createLabel(wsCandidate.getName(), true);
      panel.add(labeln1, "cell 0 1");
      panel.add(labeln2, "cell 1 1");

      if (wsCandidate.isMatchName()) {
        String wscompName = wsCandidate.getName().toLowerCase();
        String normalWName = Util.getChemicalName(Util.splitChemicalName(wscompName));

        Set<String> compSynList = beanComp.getSynList();
        for (String compSyn : compSynList) {
          compSyn = compSyn.toLowerCase();
          String cleanSyn = mtAlphaNum.reset(compSyn).replaceAll("");

          if (cleanSyn.equals(mtAlphaNum.reset(wscompName).replaceAll(""))) {
            labeln1.setForeground(Color.red);
            labeln2.setForeground(Color.red);
          }
          else {
            String normalSyn = Util.getChemicalName(Util.splitChemicalName(compSyn));
            if (normalSyn.equals(normalWName)) {
              labeln1.setForeground(Color.red);
              labeln2.setForeground(Color.red);
            }
          }
        }
      }

      int nextCell = 2;

      Set<String> compSynList = beanComp.getSynList(); // incluye nombre
      Set<String> wscompSynList = wsCandidate.getSynList();

      for (String wscompSyn : wscompSynList) {
        wscompSyn = wscompSyn.toLowerCase();

        JLabel label1 = createTitleLabel("SYN: ");
        JLabel label2 = createLabel(wscompSyn, true);
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;

        if (wsCandidate.isMatchName()) {
          String normalWName = Util.getChemicalName(Util.splitChemicalName(wscompSyn));
          for (String compSyn : compSynList) {
            compSyn = compSyn.toLowerCase();
            String cleanSyn = mtAlphaNum.reset(compSyn).replaceAll("");
            if (cleanSyn.equals(mtAlphaNum.reset(wscompSyn).replaceAll(""))) {
              label1.setForeground(Color.red);
              label2.setForeground(Color.red);
            }
            else {
              String normalSyn = Util.getChemicalName(Util.splitChemicalName(compSyn));
              if (normalSyn.equals(normalWName)) {
                label1.setForeground(Color.red);
                label2.setForeground(Color.red);
              }
            }
          }
        }
      }

      if (wsCandidate.getChebiId() != null) {
        JLabel label1c = createTitleLabel("CHEBI: ");
        Link label2c = new Link(wsCandidate.getChebiId(), getChebiURL(wsCandidate.getChebiId()));
        panel.add(label1c, "cell 0 " + nextCell);
        panel.add(label2c, "cell 1 " + nextCell);
        if (wsCandidate.isMatchChebiId()) {
          label1c.setForeground(Color.red);
          label2c.setForeground(Color.red);
        }
        nextCell++;
      }

      if (wsCandidate.getKeggId() != null) {
        JLabel label1k = createTitleLabel("KEGG: ");
        Link label2k = new Link(wsCandidate.getKeggId(), getKeggURL(wsCandidate.getKeggId()));
        panel.add(label1k, "cell 0 " + nextCell);
        panel.add(label2k, "cell 1 " + nextCell);
        if (wsCandidate.isMatchKeggId()) {
          label1k.setForeground(Color.red);
          label2k.setForeground(Color.red);
        }
        nextCell++;
      }

      // Inchi
      if (wsCandidate.getInchi() != null) {
        JLabel label1 = createTitleLabel("INCHI: ");
        JLabel label2 = createLabel(wsCandidate.getInchi(), false);
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;
        if (wsCandidate.isMatchInchi()) {
          label1.setForeground(Color.red);
          label2.setForeground(Color.red);
        }
      }
      if (wsCandidate.getInchiKey() != null) {
        JLabel label1 = createTitleLabel("INCHIKEY: ");
        JLabel label2 = createLabel(wsCandidate.getInchiKey(), false);
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;
      }

      // Smiles
      if (wsCandidate.getSmiles() != null) {
        JLabel label1 = createTitleLabel("SMILES: ");
        JLabel label2 = createLabel(wsCandidate.getSmiles(), false);
        panel.add(label1, "cell 0 " + nextCell);
        panel.add(label2, "cell 1 " + nextCell);
        nextCell++;
        if (wsCandidate.isMatchSmiles()) {
          label1.setForeground(Color.red);
          label2.setForeground(Color.red);
        }
      }

      // Formula
      Set<String> fmList = wsCandidate.getFormulaList().get(Util.SOURCE_FORMULA_CHEBI);
      if (fmList != null) {
        for (String fm : fmList) {
          JLabel label2 = createLabel(fm, true);
          JLabel label1 = createTitleLabel("FORMULA: ");
          if (wsCandidate.isMatchFormula() && fm.equals(beanComp.getSbmlFormula())) {
            label1.setForeground(Color.red);
            label2.setForeground(Color.red);
          }
          panel.add(label1, "cell 0 " + nextCell);
          panel.add(label2, "cell 1 " + nextCell);
          nextCell++;
        }
      }

      // Carga
      JLabel label1 = createTitleLabel("CARGA: ");
      JLabel label2 = createLabel(String.valueOf(wsCandidate.getCharge()));
      panel.add(label1, "cell 0 " + nextCell);
      panel.add(label2, "cell 1 " + nextCell);
      nextCell++;
      if (beanComp.getCharge() != 0 && beanComp.getCharge() == wsCandidate.getCharge()) {
        label1.setForeground(Color.red);
        label2.setForeground(Color.red);
      }

      idx++;
    } // Fin for wscomp

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

  public String getChebiURL(String query) {
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

  public String getGoogleURL(String query) {
    String url = "http://www.google.com/search?q=";
    url += query.replaceAll("\\s", "+");

    return url;
  }

  private void executeAction(ActionEvent ev,
          JPanel panel, List<WSCompound> list, BeanCompound beanComp, WSCompound wsCandidate) {

    idxDbSelected = IDX_NONE;
    dbCandidateSelected = null;

    String cmm = ev.getActionCommand();
    if (cmm.equals("NONE")) {
      idxWsSelected = IDX_NONE;
      wsCandidateSelected = null;

      spDb.setVisible(false);
    }
    else {
      idxWsSelected = Integer.parseInt(cmm);
      wsCandidateSelected = list.get(idxWsSelected);

      BeanCandidate candidate = beanComp.getBeanCandidate(wsCandidate);
      if (candidate.isDbCurated()) {
        // Save
        List<WSCompound> dblist = candidate.getDbCandidateList();
        if (!dblist.isEmpty()) {
          dbCandidateSelected = candidate.getDbCandidateList().get(0);

          updateDbInfoPanel(beanComp, dbCandidateSelected);
          spDb.setVisible(true);
        }
        else {
          spDb.setVisible(false);
        }
      }
      else {
        spDb.setVisible(true);
        updateDbPanel(beanComp, wsCandidate);
      }
    }

    panelWsSelected.setBorder(null);
    panelWsSelected = panel;
    panel.setBorder(new LineBorder(Color.red));

    validate();
    repaint();

    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "WS idx: {0} >>> {1}", new Object[]{idxWsSelected, wsCandidateSelected});
      LOGGER.log(Level.FINEST, "DB idx: {0} >>> {1}", new Object[]{idxDbSelected, dbCandidateSelected});
    }
  }

  private void executeAction(ActionEvent ev, JPanel panel, List<WSCompound> list) {
    String cmm = ev.getActionCommand();
    if (cmm.equals("NONE")) {
      idxDbSelected = IDX_NONE;
      dbCandidateSelected = null;
    }
    else {
      idxDbSelected = Integer.parseInt(cmm);
      dbCandidateSelected = list.get(idxDbSelected);
    }

    panelDbSelected.setBorder(null);
    panelDbSelected = panel;
    panel.setBorder(new LineBorder(Color.red));

    validate();
    repaint();

    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.log(Level.FINEST, "WS idx: {0} >>> {1}", new Object[]{idxWsSelected, wsCandidateSelected});
      LOGGER.log(Level.FINEST, "DB idx: {0} >>> {1}", new Object[]{idxDbSelected, dbCandidateSelected});
    }
  }

  /**
   * @return WSCompound the WsCandidate selected.
   */
  public WSCompound getWsCandidateSelected() {
    return wsCandidateSelected;
  }

  /**
   * @return WSCompound the DbCandidate selected.
   */
  public WSCompound getDbCandidateSelected() {
    return dbCandidateSelected;
  }

  public int getWsCandidateIdxSelected() {
    return idxWsSelected;
  }

  public int getDbCandidateIdxSelected() {
    return idxDbSelected;
  }
}
