package com.naomichan.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.naomichan.pos.domain.*
import com.naomichan.pos.ui.*
import com.naomichan.pos.ui.theme.NaomiPosTheme
import java.text.NumberFormat
import java.util.Locale

class MainActivity:ComponentActivity(){
 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  setContent{
   NaomiPosTheme{
    val vm:PosViewModel=viewModel(factory=PosViewModel.Factory((application as PosApplication).repository))
    PosApp(vm)
   }
  }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosApp(vm:PosViewModel){
 val s by vm.state.collectAsStateWithLifecycle()
 Scaffold(
  topBar={TopBar(s.section)},
  bottomBar={Nav(s.section,vm::section)},
  containerColor=MaterialTheme.colorScheme.background
 ){padding->
  Box(Modifier.fillMaxSize().padding(padding)){
   when(s.section){
    PosSection.SELL->Sell(vm,s)
    PosSection.TRANSACTIONS->Module("Transactions","Review completed sales, refunds and transaction history.",Icons.Default.ReceiptLong)
    PosSection.PRODUCTS->Module("Products","Catalogue, pricing, SKUs and availability.",Icons.Default.Inventory2)
    PosSection.TILL->Module("Till","Open, reconcile and close the current trading session.",Icons.Default.PointOfSale)
    PosSection.SETTINGS->Module("Settings","Device, printer, connectivity and security.",Icons.Default.Settings)
   }
   s.completed?.let{sale->
    AlertDialog(
     onDismissRequest=vm::dismiss,
     title={Text("Sale completed")},
     text={Column{
      Text(sale.reference,fontWeight=FontWeight.Bold)
      Spacer(Modifier.height(8.dp))
      Text(money(sale.totalPence),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
      Text(sale.itemCount.toString()+" items • "+sale.payment.name.lowercase().replaceFirstChar{it.uppercase()})
     }},
     confirmButton={Button(onClick=vm::dismiss){Text("Done")}}
    )
   }
  }
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(section:PosSection){
 TopAppBar(
  title={Column{
   Text("Naomi-Chan POS",fontWeight=FontWeight.Bold)
   Text(label(section),style=MaterialTheme.typography.labelSmall)
  }},
  navigationIcon={
   Surface(Modifier.padding(start=12.dp).size(38.dp),RoundedCornerShape(10.dp),MaterialTheme.colorScheme.primaryContainer){
    Box(contentAlignment=Alignment.Center){Text("N",fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)}
   }
  },
  actions={AssistChip(onClick={},label={Text("ONLINE")},leadingIcon={Icon(Icons.Default.Sync,null,Modifier.size(15.dp))})}
 )
}

@Composable
fun Sell(vm:PosViewModel,s:PosUiState){
 val cats=listOf("All")+s.products.map{it.category}.distinct()
 val products=s.products.filter{(s.category=="All"||it.category==s.category)&&(s.query.isBlank()||it.name.contains(s.query,true)||it.sku.contains(s.query,true))}
 Column(Modifier.fillMaxSize().padding(12.dp)){
  OutlinedTextField(s.query,vm::query,Modifier.fillMaxWidth(),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Search products or SKU")},shape=RoundedCornerShape(12.dp))
  Spacer(Modifier.height(8.dp))
  LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(cats){AssistChip(onClick={vm.category(it)},label={Text(it)})}}
  Spacer(Modifier.height(8.dp))
  Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(10.dp)){
   LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){items(products){ProductCard(it,vm::add)}}
   Basket(s,Modifier.width(160.dp),vm::add,vm::remove,vm::clear,vm::checkout)
  }
 }
}

@Composable
fun ProductCard(p:Product,add:(Product)->Unit){
 Card(onClick={add(p)},Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){
  Row(Modifier.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
   Surface(Modifier.size(42.dp),RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Icon(Icons.Default.ShoppingBag,null,tint=MaterialTheme.colorScheme.primary)}}
   Spacer(Modifier.width(9.dp))
   Column(Modifier.weight(1f)){
    Text(p.name,fontWeight=FontWeight.SemiBold,maxLines=2)
    Text(p.sku,style=MaterialTheme.typography.labelSmall)
    Text(p.stock.toString()+" in stock",style=MaterialTheme.typography.labelSmall)
   }
   Text(money(p.pricePence),fontWeight=FontWeight.Bold)
  }
 }
}

@Composable
fun Basket(s:PosUiState,m:Modifier,add:(Product)->Unit,remove:(Product)->Unit,clear:()->Unit,checkout:(PaymentMethod)->Unit){
 Card(m.fillMaxHeight(),shape=RoundedCornerShape(14.dp)){
  Column(Modifier.fillMaxSize().padding(10.dp)){
   Row{Icon(Icons.Default.ShoppingCart,null,Modifier.size(18.dp));Spacer(Modifier.width(5.dp));Text("Basket",fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(s.basket.itemCount.toString())}
   HorizontalDivider(Modifier.padding(vertical=7.dp))
   if(s.basket.lines.isEmpty()){
    Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center){Text("Empty",style=MaterialTheme.typography.labelMedium)}
   }else{
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
     items(s.basket.lines){line->
      Column{
       Text(line.product.name,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold,maxLines=2)
       Row(verticalAlignment=Alignment.CenterVertically){
        IconButton({remove(line.product)},Modifier.size(27.dp)){Icon(Icons.Default.Remove,null,Modifier.size(14.dp))}
        Text(line.quantity.toString())
        IconButton({add(line.product)},Modifier.size(27.dp)){Icon(Icons.Default.Add,null,Modifier.size(14.dp))}
        Spacer(Modifier.weight(1f));Text(money(line.total),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
       }
      }
     }
    }
    HorizontalDivider(Modifier.padding(vertical=7.dp))
    Row{Text("Total",fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));Text(money(s.basket.totalPence),fontWeight=FontWeight.Bold)}
    Spacer(Modifier.height(7.dp))
    Button({checkout(PaymentMethod.CARD)},Modifier.fillMaxWidth(),enabled=!s.processing){Text("Card")}
    Spacer(Modifier.height(5.dp))
    OutlinedButton({checkout(PaymentMethod.CASH)},Modifier.fillMaxWidth(),enabled=!s.processing){Text("Cash")}
    TextButton(clear,Modifier.align(Alignment.End)){Text("Clear")}
   }
  }
 }
}

@Composable
fun Module(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector){
 Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  Row(verticalAlignment=Alignment.CenterVertically){
   Surface(Modifier.size(52.dp),RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Icon(icon,null,tint=MaterialTheme.colorScheme.primary)}}
   Spacer(Modifier.width(14.dp))
   Column{Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Naomi-Chan POS",style=MaterialTheme.typography.labelMedium)}
  }
  Card{Column(Modifier.padding(18.dp)){Text("Module ready",fontWeight=FontWeight.SemiBold);Spacer(Modifier.height(4.dp));Text(subtitle)}}
 }
}

@Composable
fun Nav(current:PosSection,select:(PosSection)->Unit){
 NavigationBar{
  listOf(
   PosSection.SELL to Icons.Default.PointOfSale,
   PosSection.TRANSACTIONS to Icons.Default.ReceiptLong,
   PosSection.PRODUCTS to Icons.Default.Inventory2,
   PosSection.TILL to Icons.Default.Assessment,
   PosSection.SETTINGS to Icons.Default.Settings
  ).forEach{(x,i)->NavigationBarItem(selected=current==x,onClick={select(x)},icon={Icon(i,null)},label={Text(label(x))})}
 }
}

fun label(x:PosSection)=when(x){
 PosSection.SELL->"Sell";PosSection.TRANSACTIONS->"History";PosSection.PRODUCTS->"Products";PosSection.TILL->"Till";PosSection.SETTINGS->"Settings"
}
fun money(p:Long)=NumberFormat.getCurrencyInstance(Locale.UK).format(p/100.0)
