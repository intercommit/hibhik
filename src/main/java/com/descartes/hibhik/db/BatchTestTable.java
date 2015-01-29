package com.descartes.hibhik.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Table for batch-insert testing, registered in src/main/resources/META-INF/persistence.xml
 * Registration in "persistence.xml" does not appear to be mandatory, Hibernate finds this entity-class auto-magically.
 * @author FWiers
 *
 */
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name="table_batch_test")
public class BatchTestTable {

	private Long id;
	private String name;
	
	@Id
	@Column(name="id")
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	
	@Column(name="name")
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override public String toString() {
		return this.getClass().getSimpleName() + " " + getId() + " / " + getName();
	}
	
}
