$version: "2"

namespace com.example

use aws.auth#sigv4
use aws.protocols#restJson1

/// Allows users to retrieve a menu, create a coffee order, and
/// and to view the status of their orders
@title("Coffee Shop Service")
@sigv4(name: "coffeeshop")
@restJson1
service CoffeeShop {
    version: "2024-08-23"
    operations: [
        GetMenu
    ]
    resources: [
        Order
    ]
}

/// Retrieve the menu
@http(method: "GET", uri: "/menu")
@readonly
operation GetMenu {
    output := {
        items: CoffeeItems
    }
}
