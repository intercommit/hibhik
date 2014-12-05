package com.descartes.hibhik;

import javax.persistence.EntityManager;

/**
 * Unit test helper interface to group database actions using an entity manager.
 * @author FWiers
 *
 */
public interface DbAction {
	
	void toDb(EntityManager em);

}
