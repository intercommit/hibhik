package com.descartes.hibhik;

import javax.persistence.EntityManager;

public interface DbAction {
	
	void toDb(EntityManager em);

}
