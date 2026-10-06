package com.naomichan.pos.domain

data class Product(val id:String,val sku:String,val name:String,val category:String,val pricePence:Long,val stock:Int,val active:Boolean=true)
data class BasketLine(val product:Product,val quantity:Int){ val total:Long get()=product.pricePence*quantity }
data class Basket(val lines:List<BasketLine> = emptyList()){
  val itemCount get()=lines.sumOf{it.quantity}
  val totalPence get()=lines.sumOf{it.total}
}
enum class PaymentMethod { CARD,CASH,OTHER }
enum class PosSection { SELL,TRANSACTIONS,PRODUCTS,TILL,SETTINGS }
data class CompletedSale(val reference:String,val totalPence:Long,val itemCount:Int,val payment:PaymentMethod)
