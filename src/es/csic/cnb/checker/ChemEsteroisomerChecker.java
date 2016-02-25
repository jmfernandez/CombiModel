package es.csic.cnb.checker;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.util.DiffMatchPatch;

public final class ChemEsteroisomerChecker {
  private final Matcher mtEsteroisomer = Pattern.compile("\\b[LD]\\b").matcher("");
  private final Matcher mtInfoEsteroisomer = Pattern.compile("\\b([LD]\\b.?\\w?\\w?)").matcher("");

  private String compName;

  public ChemEsteroisomerChecker(NetCompound comp) {
    compName = getCompoundBestSyn(comp);
  }

  public boolean check(Set<String> synList) {
    boolean ok = true;

    if (compName != null) {
      // Buscar mejor candidato en synList
      final String syn = getBestSyn(compName, synList);
      if (syn != null) {
        final List<String> cList = new LinkedList<String>();
        boolean cfound = mtInfoEsteroisomer.reset(compName).find();
        while (cfound) {
          final String ctxt = mtInfoEsteroisomer.group(1);
          cList.add(ctxt);

          cfound = mtInfoEsteroisomer.find(mtInfoEsteroisomer.end(1));
        }

        final List<String> sList = new LinkedList<String>();
        boolean sfound = mtInfoEsteroisomer.reset(syn).find();
        while (sfound) {
          final String stxt = mtInfoEsteroisomer.group(1);
          sList.add(stxt);

          sfound = mtInfoEsteroisomer.find(mtInfoEsteroisomer.end(1));
        }

        // Comparar
        final int csize = cList.size();
        final int ssize = sList.size();
        if (csize > 0 && ssize > 0) {
          if (csize == ssize) {
            int matches = 0;
            for (int i=0; i<csize; i++) {
              final String cval = cList.get(i);
              final String sval = sList.get(i);

              if (cval.charAt(0) == sval.charAt(0)) {
                matches++;
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
              for (String sval : sList) {
                if (cval.equalsIgnoreCase(sval)) {
                  matches++;
                  break;
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
      if (containsEsteroisomer(syn)) {
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
    if (containsEsteroisomer(compName)) {
      return compName;
    }
    return null;
  }

  private boolean containsEsteroisomer(String syn) {
    return mtEsteroisomer.reset(syn).find();
  }
}