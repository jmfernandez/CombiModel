package es.csic.cnb.checker;

import java.util.ArrayList;
import java.util.List;

import org.sbml.jsbml.Species;

import chemaxon.formats.MolFormatException;
import chemaxon.license.LicenseManager;
import chemaxon.license.LicenseProcessingException;
import chemaxon.marvin.calculations.pKaPlugin;
import chemaxon.marvin.plugin.PluginException;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.util.MolHandler;
import es.csic.cnb.data.NetCompound;
import es.csic.cnb.data.WSCompound;
import es.csic.cnb.util.Util;
import es.csic.cnb.ws.ChebiException;
import es.csic.cnb.ws.ChebiWebService;

public class PkaChecker {
  private pKaPlugin plugin;

  public PkaChecker() throws LicenseProcessingException {
    try {
      String lpath = this.getClass().getResource("/configuration/license.cxl").getPath();
      LicenseManager.setLicenseFile(lpath);
    } catch (LicenseProcessingException e) {
      StringBuilder sb = new StringBuilder();
      sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
      sb.append("<!--PLEASE DO NOT MODIFY THIS FILE AND BE AWARE THAT THE FILE CONTAINS CONFIDENTIAL INFORMATION.-->");
      sb.append("<ChemAxon>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Marvin Applets\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFFVMA2NZMiSMWvGMLHZ8EjWANE08AhRMxM3ss5L1HnHjCP4s65ag6jh7Xw==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Marvin Beans\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MC0CFQCMr4ohAfhsh1VVxPXBGJhUa6OmKQIUENhwX3uUN/OHnEhW2bx7IAilLYU=\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Protonation Plugin Group\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MC0CFQCThvzZezDih78ztHM/1FOwRGJo8QIUAkua7p0d986mhMEzxNdudd5CkTI=\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Partitioning Plugin Group\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFCGDbrEqDjc0JMbukVmlVo0ZhD6YAhQ2qqDiio6z4XuOEsWrm6mSNmCa6A==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Charge Plugin Group\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MC0CFQCE5f3v+36k7FRSiWKhg+iyDOX0QgIUE9B9ul0Vy2WlcqwGf9gUW51Dopg=\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Isomers Plugin Group\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFCQHC1NHFhRFl1hRGocmDNn/kCPDAhRQG6nTDI+2MuoxLZ6ITk7rLMetyQ==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Conformation Plugin Group\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"B/S/P/E\" Value=\"Enterprise\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFAVGQKyVfqeynzeyE8v+LC+8kAKMAhR7JM5OVPGn28+3g1OY9dU7AhVBWw==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Geometry Plugin Group\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFCHGl4kIsfQoJ0n843m0wfe+qVV8AhRhAO2kCOPPjtmbrS1fKWyP0D/Jww==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Huckel Analysis Plugin\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFHhB7jvEnTWoisfrNqR64NQ0gs8QAhRwDM+TrI6+pe3O59DnW872bCrI3w==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Refractivity Plugin\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MC0CFBaQ+j81QSc+N3zNOPPosjVfJnTLAhUAhEWcO9cdBR/6hJXLC9lqcVmQf14=\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"HBDA Plugin\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFCAagzr0UceVrkXBxQFqZZLEjFi7AhQb3rLb9BEwe+7lTJuWPGNuik/QKw==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Markush Enumeration Plugin\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MC0CFQCJJp8FGWdwGFA8ZpMObZxWgd7ZhgIUG74Fuuz5PSg5t49+j/2G+K9FEz4=\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Name to Structure\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MC0CFQCRfAgv7G144+geVC6pflHC/OGFSgIUaF6sYhLFSmzkWk6KhzmQ9WOWjGc=\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Structural Frameworks Plugin\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFEO7WzCjxqjvJW75BlEdP74m+li4AhRQHnKE9B4gQdk6re7evkSQmOiAFA==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Structure Checker\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MC0CFQCMreoe/V4vXJsAco0reyy06rs+0AIUbuHVvBAS9gnCTTbb/pYeWEr3WOY=\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Predictor Plugin\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFAdM8t02lFwqzL8CVsJigaE1ESzKAhQKrvLVriJ9wgBqg/EHCicH/URsXg==\"/>");
      sb.append("</License>");
      sb.append("<License>");
      sb.append("<Field Name=\"Software\" Value=\"Document to Structure\"/>");
      sb.append("<Field Name=\"License Term\" Value=\"Evaluation\"/>");
      sb.append("<Field Name=\"Licensee\" Value=\"CNB\"/>");
      sb.append("<Field Name=\"Number of Users\" Value=\"Unlimited\"/>");
      sb.append("<Field Name=\"Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Support Expiration Date\" Value=\"2013-07-30\"/>");
      sb.append("<Field Name=\"Comment\" Value=\"Marvin 5.7 evaluation bundle autogenerated\"/>");
      sb.append("<Restriction Name=\"Server Use\" Value=\"Not Allowed\"/>");
      sb.append("<Signature Value=\"MCwCFB8yDjfrm/ixv/E5FbnwWaLSNETGAhRa3iOvCd59B4Npck/CnOJDyyN3Mw==\"/>");
      sb.append("</License>");
      sb.append("</ChemAxon>");

      LicenseManager.setLicense(sb.toString());
    }

    // instantiate plugin
    plugin = new pKaPlugin();
  }

  @SuppressWarnings("deprecation")
  public NetCompound check(NetCompound comp, double ph) {
    NetCompound newComp = null;

    // Candidatos
    List<WSCompound> candidateList = new ArrayList<WSCompound>();

    String structure = comp.getStructure();
    if (structure != null && ph != Util.DEFAULT_PH) {
      MolHandler h;
      try {
        h = new MolHandler(structure);
        Molecule mol = h.getMolecule();

        // set molecule and run calculation
        plugin.setMolecule(mol);
        plugin.run();

        // get the acidic and basic macro pKa values
        //double[] acidicpKaValues = plugin.getMacropKaValues(pKaPlugin.ACIDIC);
        //double[] basicpKaValues = plugin.getMacropKaValues(pKaPlugin.BASIC);

        // Adaptar la formula y buscar en chebi
        int count = mol.getAtomCount();
        for (int i=0; i < count; ++i) {
          MolAtom at = mol.getAtom(i);
          double pka = plugin.getpKa(i);

          if (pka < Util.DEFAULT_PH && pka > ph) {
            at.setCharge(at.getCharge()-1);
          }
          else if (pka > Util.DEFAULT_PH && pka < ph) {
            at.setCharge(at.getCharge()+1);
          }
          //System.out.println(" - " + at.getSymbol() + " (" + at.getCharge() + ") " + pka);
        }
        mol.valenceCheck();

        String newFormula = mol.getFormula();
        int newCharge = mol.getFormalCharge();

        ChebiWebService ws = new ChebiWebService();
        List<WSCompound> chebiCandidateList = ws.searchByFm(newFormula);
        for (WSCompound wscomp : chebiCandidateList) {
          if (wscomp.getCharge() == newCharge) {
            for (String chebiId : comp.getChebiIdList()) {
              if (ws.isConjugated(chebiId, wscomp.getChebiId())) {
                candidateList.add(wscomp);
              }
            }
          }
        }

        if (candidateList.size() == 1) {
          WSCompound wscomp = candidateList.get(0);

          // Actualizar la especie
          Species sp = comp.getSpecies().clone();
          sp.setParent(comp.getSpecies().getParent());
          sp.setId("tmp_" + String.valueOf((int)(Integer.MAX_VALUE * Math.random())) + "_" + comp.getCompartmentAbbrev()); // temporal
          sp.setName(wscomp.getName() + "_" + newFormula);
          sp.unsetNotes();
          Util.setChargeToSpecies(sp,newCharge,comp.isFBC());

          newComp = new NetCompound(sp,comp.isFBC());
          newComp.update(wscomp);
        }


//        // TRAZA
//        System.out.println("MOL: "+comp.getOfn()+ " ["+comp.getCharge()+"]  - PH " + ph );
//        for (int i=0; i < count; ++i) {
//          MolAtom at = mol.getAtom(i);
//          double pka = plugin.getpKa(i);
//          System.out.println(" - " + at.getSymbol() + " (" + at.getCharge() + ") " + pka);
//        }
//        System.out.println("CARGA: "+ newCharge);
//        System.out.println("FM: "+ newFormula);
//        if (newComp != null) {
//          System.out.println(comp + " ---> " + newComp);
//        }


      } catch (MolFormatException e) {
        e.printStackTrace();
      } catch (PluginException e) {
        e.printStackTrace();
      } catch (ChebiException e) {
        e.printStackTrace();
      }
    }

    return newComp;
  }
}
