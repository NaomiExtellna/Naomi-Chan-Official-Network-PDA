package com.naomichan.pos.data

import com.naomichan.pos.domain.*
import kotlinx.coroutines.delay
import java.util.UUID

class PosRepository {
  private val products = listOf(
    Product("1","NC-MUG-001","Naomi-Chan Signature Mug","Merchandise",1200,42),
    Product("2","NC-TEE-001","Naomi-Chan Classic T-Shirt","Apparel",2200,18),
    Product("3","NC-STK-001","Naomi-Chan Logo Sticker","Accessories",300,95),
    Product("4","NC-WRS-001","Naomi-Chan Wristband","Accessories",500,67),
    Product("5","NC-DIG-001","Digital DJ Mix","Digital",500,999),
    Product("6","NC-HOD-001","Limited Edition Hoodie","Apparel",4200,9)
  )
  suspend fun catalogue():List<Product>{ delay(80); return products.filter{it.active} }
  suspend fun commitSale(total:Long,count:Int,payment:PaymentMethod):CompletedSale{
    delay(120)
    return CompletedSale("NC-"+"$"+"{System.currentTimeMillis().toString().takeLast(8)}-"+"$"+"{UUID.randomUUID().toString().take(4).uppercase()}",total,count,payment)
  }
}
