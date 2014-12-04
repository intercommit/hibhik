package com.descartes.hibhik.db;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Table for testing, registered in src/main/resources/META-INF/persistence.xml
 * @author FWiers
 *
 */
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name="table_test")
public class TestTable {

	private Long id;
	private String name;
	
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
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
