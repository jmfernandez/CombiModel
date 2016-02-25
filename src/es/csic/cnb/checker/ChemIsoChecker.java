package es.csic.cnb.checker;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.data.NetCompound;
import es.csic.cnb.util.DiffMatchPatch;
import es.csic.cnb.util.Util;

public class ChemIsoChecker {
  private Matcher mtAnteiso = Pattern.compile("((ante)?iso)", Pattern.CASE_INSENSITIVE).matcher("");

  private String compName;
  private String compNameWithIso;

  public ChemIsoChecker(NetCompound comp) {
    compNameWithIso = getCompoundBestSyn(comp);
  }

  public boolean check(Set<String> synList) {
    boolean ok = true;

    // No existe iso en el compuesto
    if (compNameWithIso == null) {
      // Buscar mejor candidato con iso en synList
      String synIso = getBestIsoSyn(compName, synList);

      // Existe syn con iso
      if (synIso != null) {
        // Dividir nombre y extraer iso
        List<String> sTks = Util.splitChemicalName(synIso.toLowerCase());
        tokens:
          for (String stk : sTks) {
            if (mtAnteiso.reset(stk).find()) {
              // Quitar iso del token
              final String stkWithoutIso = mtAnteiso.reset(stk).replaceAll("");

              // Comparar
              final String cname = Util.getChemicalName(Util.splitChemicalName(compName.toLowerCase()));
              if (cname.contains(stkWithoutIso)) {
                // NO OK
                ok = false;
                break tokens;
              }
            }
          }
      }
    }

    // Existe iso en el compuesto
    else {
      // Buscar mejor candidato con iso en synList
      final String synIso = getBestIsoSyn(compNameWithIso, synList);

      // No existe syn con iso
      if (synIso == null) {
        // Dividir nombre y extraer iso
        List<String> cTks = Util.splitChemicalName(compNameWithIso.toLowerCase());
        tokens:
          for (String ctk : cTks) {
            if (mtAnteiso.reset(ctk).find()) {
              // Quitar iso del token
              final String ctkWithoutIso = mtAnteiso.reset(ctk).replaceAll("");

              // Comparar
              for (String syn : synList) {
                // Normalizar
                syn = Util.getChemicalName(Util.splitChemicalName(syn.toLowerCase()));

                if (syn.contains(ctkWithoutIso)) {
                  // NO OK
                  ok = false;
                  break tokens;
                }
              }
            }
          }
      }
      // Existe syn con iso (comprobar anteiso)
      else {
        if (mtAnteiso.reset(compNameWithIso).find()) {
          final int compl = mtAnteiso.group(1).length();
          if (mtAnteiso.reset(synIso).find()) {
            int synl = mtAnteiso.group(1).length();
            // Si el tamano del texto recuperado (iso o anteiso) es diferente
            // es porque uno es iso y otro anteiso => Compuestos diferentes
            if (compl != synl) {
              // NO OK
              ok = false;
            }
          }
        }
      }
    }

    return ok;
  }

  private String getBestIsoSyn(String compName, Set<String> synList) {
    final DiffMatchPatch diff = new DiffMatchPatch();

    String bestSyn = null;
    int mlv = 100;
    for (String syn : synList) {
      if (containsIso(syn)) {
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
    compName = (comp.getSbmlName() == null) ? "" : comp.getSbmlName();

    if (containsIso(compName)) {
      return compName;
    }
    return null;
  }

  private boolean containsIso(String syn) {
    return mtAnteiso.reset(syn).find();
  }
}