package br.com.bbts.catalog.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 64)
	private String sku;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(length = 1000)
	private String description;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private boolean active;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	public Product(String sku, String name, String description, BigDecimal price, boolean active, Category category) {
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.price = price;
		this.active = active;
		this.category = category;
	}

}
