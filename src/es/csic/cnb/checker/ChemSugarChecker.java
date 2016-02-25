package es.csic.cnb.checker;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.csic.cnb.data.NetCompound;

public class ChemSugarChecker {
  private Matcher mtSugar = Pattern.compile("(gluco|galacto|fructo|malto|lacto|fuco|ribulo|ribo|arabino|xylulo|manno|rhamno|talo|cellobio|trehalo|tagato)",
          Pattern.CASE_INSENSITIVE).matcher("");

  private String sugar;

  public ChemSugarChecker(NetCompound comp) {
    final String compName = (comp.getSbmlName() == null) ? "" : comp.getSbmlName();

    if (mtSugar.reset(compName).find()) {
      sugar = mtSugar.group(1);
    }
  }

  public boolean check(Set<String> synList) {
    boolean ok = true;

    if (sugar != null) {
      // Ver si existe en el syn
      for (String syn : synList) {
        if (mtSugar.reset(syn).find()) {
          final String synSugar = mtSugar.group(1);

          if (!sugar.equalsIgnoreCase(synSugar)) {
            // NO OK
            ok = false;
            break;
          }
        }
      }
    }

    return ok;
  }
}
