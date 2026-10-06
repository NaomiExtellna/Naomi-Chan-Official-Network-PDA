package com.naomichan.pos.ui

import androidx.lifecycle.*
import com.naomichan.pos.data.PosRepository
import com.naomichan.pos.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PosUiState(
  val section:PosSection=PosSection.SELL,
  val products:List<Product> = emptyList(),
  val basket:Basket=Basket(),
  val category:String="All",
  val query:String="",
  val loading:Boolean=true,
  val processing:Boolean=false,
  val completed:CompletedSale?=null
)
class PosViewModel(private val repo:PosRepository):ViewModel(){
  private val _state=MutableStateFlow(PosUiState())
  val state:StateFlow<PosUiState> = _state.asStateFlow()
  init{refresh()}
  fun refresh(){viewModelScope.launch{_state.update{it.copy(loading=true)};_state.update{it.copy(products=repo.catalogue(),loading=false)}}}
  fun section(v:PosSection)=_state.update{it.copy(section=v)}
  fun query(v:String)=_state.update{it.copy(query=v)}
  fun category(v:String)=_state.update{it.copy(category=v)}
  fun add(p:Product){_state.update{s->val l=s.basket.lines.toMutableList();val i=l.indexOfFirst{it.product.id==p.id};if(i<0)l+=BasketLine(p,1)else l[i]=l[i].copy(quantity=l[i].quantity+1);s.copy(basket=Basket(l))}}
  fun remove(p:Product){_state.update{s->val l=s.basket.lines.toMutableList();val i=l.indexOfFirst{it.product.id==p.id};if(i<0)s else{if(l[i].quantity==1)l.removeAt(i)else l[i]=l[i].copy(quantity=l[i].quantity-1);s.copy(basket=Basket(l))}}}
  fun clear(){_state.update{it.copy(basket=Basket())}}
  fun checkout(method:PaymentMethod){val s=_state.value;if(s.basket.lines.isEmpty()||s.processing)return;viewModelScope.launch{_state.update{it.copy(processing=true)};val sale=repo.commitSale(s.basket.totalPence,s.basket.itemCount,method);_state.value=s.copy(basket=Basket(),processing=false,completed=sale)}}
  fun dismiss(){_state.update{it.copy(completed=null)}}
  class Factory(private val repo:PosRepository):ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun <T:ViewModel>create(c:Class<T>):T=PosViewModel(repo) as T}
}
