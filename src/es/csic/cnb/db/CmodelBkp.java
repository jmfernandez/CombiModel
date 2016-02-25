package es.csic.cnb.db;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CmodelBkp {
	private final static Logger LOGGER = Logger.getLogger(CmodelBkp.class.getName());
	
	private DbManager dbMgr = DbManager.INSTANCE;
	
	public void runBkp() {
		try {
			dbMgr.getCompoundDbMgr().scriptTo();
			LOGGER.info("Compound database backup executed");
			
			dbMgr.getKeggDbMgr().scriptTo();
			LOGGER.info("Kegg database backup executed");
			
			dbMgr.getSeedDbMgr().scriptTo();
			LOGGER.info("Seed database backup executed");
			
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public static void main(String[] args) {
		CmodelBkp bkp = new CmodelBkp();
		bkp.runBkp();		
	}
}
