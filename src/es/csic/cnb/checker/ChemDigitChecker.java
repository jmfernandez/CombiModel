package es.csic.cnb.checker;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.util.DiffMatchPatch;
import es.csic.cnb.util.Util;

public final class ChemDigitChecker {
  private final Matcher mtDigit = Pattern.compile("\\b\\d+").matcher("");
  private final Matcher mtEmbededDigit = Pattern.compile("\\d+").matcher("");
  private static Matcher mtEmbededDigitCorrector = Pattern.compile("[a-z](\\d+)").matcher("");
  private final Matcher mtFirstDigit = Pattern.compile("^[RSLD]?\\d{1,8}(?:[A-Za-z]|$)").matcher("");
  private final Matcher mtInfoDigit1 = Pattern.compile("^[RSLD]?(\\d+)(\\w?)").matcher("");
  private final Matcher mtInfoDigit2 = Pattern.compile("^[RSLD]?(\\d+\\w?)").matcher("");

  private String compName;

  public ChemDigitChecker(NetCompound comp) {
    compName = getCompoundBestSyn(comp);
  }

  public boolean check(Set<String> synList) {
    boolean ok = true;

    if (compName != null) {
      // Buscar mejor candidato en synList
      final String syn = getBestSyn(compName, synList);
      if (syn != null) {
        final List<String> cList = new LinkedList<String>();
        final List<String> csplitList = Util.splitChemicalName(compName);
        for (String s : csplitList) {
          if (containsFirstDigit(s)) {
            cList.add(s);
          }
        }

        final List<String> sList = new LinkedList<String>();
        final List<String> ssplitList = Util.splitChemicalName(syn);
        for (String s : ssplitList) {
          if (containsFirstDigit(s)) {
            sList.add(s);
          }
        }

        // Comparar
        final int csize = cList.size();
        final int ssize = sList.size();
        if (csize > 0 && ssize > 0) {

          if (csize == ssize) {
            int matches = 0;
            for (String cval : cList) {
              if (mtInfoDigit1.reset(cval).find()) {
                final int cdigit = Integer.parseInt(mtInfoDigit1.group(1));
                final String ctxt = mtInfoDigit1.group(2);

                for (String sval : sList) {
                  if (mtInfoDigit1.reset(sval).find()) {
                    final int sdigit = Integer.parseInt(mtInfoDigit1.group(1));
                    final String stxt = mtInfoDigit1.group(2);

                    if (cdigit == sdigit && ctxt.length() == stxt.length()) {
                      matches++;
                      break;
                    }
                  }
                }
              }
            }
            if (matches != csize) {
              // NO OK
              ok = false;
            }
          }

          else {
            int matches = 0;
            for (String cval : cList) {
              //Matcher mch = (mtInfoDigit2.reset(cval));
              if (mtInfoDigit2.reset(cval).find()) {
                final String ctxt = mtInfoDigit2.group(1);

                for (String sval : sList) {
                  //mch = (mtInfoDigit2.reset(sval));
                  if (mtInfoDigit2.reset(sval).find()) {
                    final String stxt = mtInfoDigit2.group(1);

                    if (ctxt.equalsIgnoreCase(stxt)) {
                      matches++;
                      break;
                    }
                  }
                }
              }
            }
            if ( !((csize > ssize && matches == ssize) ||
                    (ssize > csize && matches == csize)) ) {
              // NO OK
              ok = false;
            }
          }
        }
      }
    }
    return ok;
  }

  private String getBestSyn(String compName, Set<String> synList) {
    final DiffMatchPatch diff = new DiffMatchPatch();

    String bestSyn = null;
    int mlv = 100;
    for (String syn : synList) {
      if (containsDigit(syn)) {
        final int lv = diff.diff_levenshtein(diff.diff_main(compName, syn));
        if (lv < mlv) {
          mlv = lv;
          bestSyn = syn;
        }
      }
    }
    return bestSyn;
  }

  private String getCompoundBestSyn(NetCompound comp) {
    final String compName = (comp.getSbmlName() == null) ? "" : comp.getSbmlName();

    // Establecer el mejor sinonimo para el compuesto
    if (containsDigit(compName)) {
      return compName;
    }
    // Establecer el mejor sinonimo para el compuesto
    else if (containsEmbededDigit(compName)) {
      for (String syn : comp.getSynList()) {
        if (containsDigit(syn)) {
          return syn;
        }
      }

      // Corregir digitos embebidos
      boolean found = mtEmbededDigitCorrector.reset(compName).find();
      if (found) {
        final StringBuilder sb = new StringBuilder();

        while (found) {
          int start = mtEmbededDigitCorrector.start(1);
          sb.append(compName.substring(0, start)).append(" ").append(compName.substring(start));

          found = mtEmbededDigitCorrector.reset(sb).find(start);
        }
        return sb.toString();
      }
    }
    return null;
  }

  private boolean containsFirstDigit(String syn) {
    return mtFirstDigit.reset(syn).find();
  }

  private boolean containsDigit(String syn) {
    return mtDigit.reset(syn).find();
  }

  private boolean containsEmbededDigit(String syn) {
    return mtEmbededDigit.reset(syn).find();
  }
}