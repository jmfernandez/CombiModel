package es.csic.cnb.rmi.client.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;
import es.csic.cnb.curation.BeanCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.rmi.RMIInterfaz;

public class ManualSelectionDialog extends JDialog implements ActionListener {
  private static final long serialVersionUID = 1L;

  private final JPanel contentPanel = new JPanel();

  private CompPanel compPanel;
  private OptPanel optPanel;

  private int status;

  private boolean mostrado = false;

  private JLabel lbInf;

  private RMIInterfaz rmi;
  private BeanCompound beanCompound;

  private JButton okButton;
  private JButton skipButton;

  private TitledBorder compTitle;

  /**
   * Create the dialog.
   */
  public ManualSelectionDialog(RMIInterfaz rmi) {
    this.rmi = rmi;

    this.setModal(true);
    this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

    addWindowListener(new WindowAdapter() {
      // WINDOW_CLOSING event handler
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        close();
      }
    });

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int height = (int) (screenSize.height / 1.25);
    int width = (int) (screenSize.width / 1.35);
    setSize(width, height);

    // here's the part where i center the jframe on screen
    setLocationRelativeTo(null);

    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    contentPanel.setLayout(new BorderLayout(0, 0));

    // CONTENIDO
    {
      JPanel panel = new JPanel();
      panel.setLayout(new BorderLayout(0, 0));
      {
        compPanel = new CompPanel();
        JScrollPane scPanel1 = new JScrollPane(compPanel);
        scPanel1.getVerticalScrollBar().setUnitIncrement(16);
        scPanel1.setMinimumSize(new Dimension(160, 250));
        scPanel1.setPreferredSize(new Dimension(160, 250));
        compTitle = new TitledBorder(null, "SBML Compound ", TitledBorder.LEADING, TitledBorder.TOP, null, null);
        scPanel1.setBorder(compTitle);
        panel.add(scPanel1, BorderLayout.NORTH);

        optPanel = new OptPanel();
        panel.add(optPanel, BorderLayout.CENTER);
      }
      contentPanel.add(panel);
    }

    // BOTONES
    {
      JPanel buttonPane = new JPanel();
      getContentPane().add(buttonPane, BorderLayout.SOUTH);
      {
        okButton = new JButton("Save and Next");
        okButton.setActionCommand("OK");
        okButton.addActionListener(this);
        buttonPane.setLayout(new MigLayout("", "[179.00,grow][54px][64px][59px]", "[25px]"));
        {
          lbInf = new JLabel();
          buttonPane.add(lbInf, "cell 0 0");
        }
        buttonPane.add(okButton, "cell 1 0,alignx left,aligny top");
        getRootPane().setDefaultButton(okButton);
      }
      {
        skipButton = new JButton("Skip");
        skipButton.setActionCommand("Skip");
        skipButton.setMnemonic(KeyEvent.VK_S);
        skipButton.addActionListener(this);
        buttonPane.add(skipButton, "cell 2 0,alignx left,aligny top");
      }
      {
        JButton exitButton = new JButton("Exit");
        exitButton.setActionCommand("Exit");
        exitButton.addActionListener(this);
        buttonPane.add(exitButton, "cell 3 0,alignx left,aligny top");
      }
    }
  }

  public void clean() {
    compPanel.clean();
    optPanel.clean();

    validate();
    repaint();
  }

  public void update(BeanCompound data) {
    compPanel.update(data);
    compTitle.setTitle("SBML Compound " + data.getDorsal() + " ");

    optPanel.update(data);

    validate();
    repaint();
  }

  /**
   * Muestra el dialog.
   * Puedo devolver un valor (al estar el dialog en modal no se retornara hasta que no se oculte la ventana)
   */
  public void display() {
    getNextCompound();

    setLocationRelativeTo(null);
    setVisible(true);
  }

  public int getStatus() {
    return status;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final String id = beanCompound.getSbmlCleanId();

    try {
      if ("OK".equals(e.getActionCommand())) {
        WSCompound wsCandidate = optPanel.getWsCandidateSelected();
        WSCompound dbCandidate = optPanel.getDbCandidateSelected();

        if (wsCandidate != null && dbCandidate != null) {
          rmi.saveBothCandidates(id, wsCandidate, dbCandidate);
        }
        else if (wsCandidate != null) {
          rmi.saveWsCandidate(id, wsCandidate);
        }
        else if (dbCandidate != null) {
          rmi.saveDbCandidate(id, dbCandidate);
        }
        // Casos NONE
        else {
          rmi.saveNoneCandidate(id);
        }

        getNextCompound();
      }
      else if ("Skip".equals(e.getActionCommand())) {
        if (mostrado) {
          rmi.skipCompound(id);
          getNextCompound();
        }
        else {
          mostrado = true;

          // JOption avisando de que quedara suelto y fuera de la base de datos
          int response = JOptionPane.showConfirmDialog(this,
                  "Si continua la cancelacion NO se cargaran los datos de este compuesto en la base de datos." +
                          "\n¿Continuar?",
                          "Informacion",
                          JOptionPane.YES_NO_OPTION);
          // Continuar proceso de cancelacion
          if (response == JOptionPane.YES_OPTION) {
            rmi.skipCompound(id);
            getNextCompound();
          }
        }
      }
      else if("Exit".equals(e.getActionCommand())) {
        close();
      }
    } catch (RemoteException e1) {
      e1.printStackTrace();
    }
  }

  private void close() {
    if (beanCompound != null) {
      final String id = beanCompound.getSbmlCleanId();
      try {
        rmi.exit(id);
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    mostrado = false;
    setVisible(false);
  }

  public void loader() {
    compPanel.loader();
    optPanel.loader();

    validate();
    repaint();
  }

  private void getNextCompound() {
    this.clean();

    SwingWorker<BeanCompound, Void> worker = new SwingWorker<BeanCompound, Void>() {
      Timer timer;
      @Override
      protected BeanCompound doInBackground() throws Exception {
        timer = new Timer (650, new ActionListener () {
          public void actionPerformed(ActionEvent e) {
            loader();
          }
        });
        timer.setRepeats(false);
        timer.start();

        okButton.setEnabled(false);
        skipButton.setEnabled(false);

        lbInf.setForeground(Color.RED);
        lbInf.setText("loading...");

        return rmi.getNextCompound();
      }

      @Override
      public void done() {
        timer.stop();

        try {
          beanCompound = get();

          // Si no se recupera nada se muestra mensaje
          if (beanCompound == null) {
            lbInf.setForeground(Color.RED);
            lbInf.setText("NO ENCONTRADO");
            JOptionPane.showMessageDialog(null, "No hay compuesto para curar manualmente");

            // Cerrar
            close();
          }
          else {
            okButton.setEnabled(true);
            skipButton.setEnabled(true);

            update(beanCompound);

            lbInf.setForeground(Color.BLUE);
            lbInf.setText("<html><b style='color:#002FFF;'>Comp nº " + beanCompound.getDorsal() +
                    "</b> - Curated: " + rmi.getTotalManual() + " man. + " +
                    rmi.getTotalAuto() + " auto." +
                    " [Remain: " + (rmi.getTotal() + 1) + "]</html>");
          }
        } catch (Exception e) {
          e.printStackTrace();

          JOptionPane.showMessageDialog(null, e.getMessage());

          // Cerrar
          close();
        }
      }
    };
    worker.execute();
  }

///**
//* Launch the application.
//*/
//public static void main(String[] args) {
// try {
//
//   ManualSelectionDialog dialog = new ManualSelectionDialog();
//   //dialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
//   dialog.setVisible(true);
//
//
//
// } catch (Exception e) {
//   e.printStackTrace();
// }
//}
}
