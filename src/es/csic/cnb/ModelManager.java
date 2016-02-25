package es.csic.cnb;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sbml.jsbml.Species;

import es.csic.cnb.ws.ChebiException;

public class ModelManager {
  private ModelParsing modelParser;

  public ModelManager() {
    this.modelParser = new ModelParsing();
  }

  /**
   * Metodo que carga y normaliza un modelo.
   *
   * @param file String
   * @param modelAtts
   * @throws ChebiException
   */
  public void loadModel(String file, Map<String,String> modelAtts) throws ChebiException {
    this.loadModel(new File(file), modelAtts);
  }

  /**
   * Metodo que carga y normaliza un modelo.
   *
   * @param file File
   * @param modelAtts
   * @throws ChebiException
   */
  public void loadModel(File file, Map<String,String> modelAtts) throws ChebiException {
    modelParser.parseSBML(file, modelAtts);
    modelParser.normalizeSBML(false); // No retorna nada porque al ser false no se calculan las equivalencias
  }

  /**
   * Metodo que carga modelo sin normalizarlo y lo vuelve a volcar a formato xml.
   * @param file
   * @param fileOut
   * @param modelAtts
   */
  public void parseAndWriteModel(File file, File fileOut, Map<String,String> modelAtts) {
    modelParser.parseSBML(file, modelAtts);
    modelParser.writeNormalSBML(fileOut);
  }

//  public File writeNormalizedModel(File file, Map<String,String> modelAtts) throws ChebiException {
//    File out = new File(file.getParent(), "normal_" + file.getName());
//    this.writeNormalizedModel(file, out, modelAtts);
//    return out;
//  }

  public List<String> writeNormalizedModel(File fileIn, File fileOut) throws ChebiException {
    Map<String,String> modelAtts = Collections.emptyMap();
    return this.writeNormalizedModel(fileIn, fileOut, modelAtts);
  }

  public List<String> writeNormalizedModel(File fileIn, File fileOut, Map<String,String> modelAtts) throws ChebiException {
    modelParser.parseSBML(fileIn, modelAtts);
    List<String> equivList = modelParser.normalizeSBML(true);
    modelParser.writeNormalSBML(fileOut);

    return equivList;
  }

  public List<Species> writeManualCurationModel() {
    return modelParser.writeManualCurationSBML();
  }

  public String validateSBML(String filepath) {
    return modelParser.validateSBML(filepath);
  }
}
