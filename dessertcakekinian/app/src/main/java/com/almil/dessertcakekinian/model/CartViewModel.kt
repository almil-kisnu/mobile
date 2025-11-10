
    package com.almil.dessertcakekinian.model

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.LiveData
    import androidx.lifecycle.MutableLiveData

    class CartViewModel : ViewModel() {
        private val _cartItems = MutableLiveData<MutableMap<Int, CartItem>>()

        val cartItems: LiveData<MutableMap<Int, CartItem>> = _cartItems

        init {
            _cartItems.value = mutableMapOf()
        }
        private fun getAdjustedPrice(produkDetail: ProdukDetail, quantity: Int): Double {
            val hargaGrosirList = produkDetail.hargaGrosir

            val matchedGrosir = hargaGrosirList
                .filter { it.minQty > 0 && quantity >= it.minQty }
                .maxByOrNull { it.minQty }

            return matchedGrosir?.hargaJual ?: produkDetail.produk.harga_eceran ?: 0.0
        }

        fun updateCart(idproduk: Int, produkDetail: ProdukDetail, quantity: Int) {
            val currentMap = _cartItems.value ?: mutableMapOf()

            if (quantity > 0) {
                val newHargaSatuan = getAdjustedPrice(produkDetail, quantity)

                val existingItem = currentMap[idproduk]

                if (existingItem != null) {
                    existingItem.quantity = quantity
                    existingItem.hargaSatuan = newHargaSatuan
                } else {
                    currentMap[idproduk] = CartItem(produkDetail, quantity, newHargaSatuan)
                }
            } else {
                currentMap.remove(idproduk)
            }

            _cartItems.value = currentMap
        }

        fun clearCart() {
            _cartItems.value = mutableMapOf()
        }

        fun getCartQuantitiesMap(): Map<Int, Int> {
            return _cartItems.value?.mapValues { it.value.quantity } ?: emptyMap()
        }
    }