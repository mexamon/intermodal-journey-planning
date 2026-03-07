package com.thy.cloud.data.jpa.specs.alternate;
/**
 * operator enumeration
 *
 */
public enum Operator {
	/** equals = */
	EQUAL,
	/** not equal to <> */
	NOT_EQUAL,
	/** greater than or equal to >= */
	GE,
	/** less than or equal to <= */
	LE,
	/** greater than > */
	GT,
	/** less than < */
	LT,
	/** Full like %{username}% */
	LIKE,
	/** Right like {username}% */
	R_LIKE,
	/** left like %{username} */
	L_LIKE,
	/** Is empty */
	ISNULL,
	/** not null */
	IS_NOTNULL,
	/** between */
	BETWEEN
}