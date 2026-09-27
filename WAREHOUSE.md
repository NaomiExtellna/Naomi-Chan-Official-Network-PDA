# Naomi-Chan™ BFC Warehouse for SUNMI V2

This branch converts the Naomi-Chan Official Network PDA from its former POS/event workflow into a compact BFC Fulfilment Centre handheld.

## Warehouse workflow

The primary navigation is now:

- **Home** — BFC command summary for pick/pack, dispatch, returns, low stock and open holds.
- **Pick** — warehouse queue with bin locations, line-by-line pick confirmation, packing and dispatch transitions.
- **Scan** — CameraX + bundled ML Kit lookup for order references, parcel tracking numbers and stock SKUs, with manual entry fallback.
- **Stock** — fast physical stock adjustments and low-stock highlighting.
- **More** — returns intake, operational holds, secure terminal information and terminal lock.

The terminology follows the website BFC workflow:

Processing → Packed → Shipped → In transit → Out for delivery → Delivered

## Website-matched design

The Compose palette mirrors the Naomi-Chan staff workspace:

- workspace background: #F6F7F9
- primary ink/action: #101828
- muted text: #667085
- borders: #E4E7EC
- panels: white / #FAFBFC
- staff accent: #31556A
- subtle identity accents: trans blue #5BCEFA and trans pink #F5A9B8

This intentionally replaces the older red/orange POS treatment with the quieter staff/BFC design language.

## SUNMI V2 decisions

- Keeps minSdk 24; SUNMI V2 / Android 7.1.x remains inside the supported range.
- Keeps classic window insets rather than edge-to-edge mode for older SUNMI SystemUI.
- Reuses the existing local Room staff authentication and audit trail.
- Reuses the existing CameraX/ML Kit stack, but reduces the warehouse scanner analysis target to 640×480 to reduce work on the V2.
- Leaves the existing SUNMI printer stack in the project so packing-slip/label printing can be wired to BFC records without replacing the proven device integration.
- Keeps the existing Android application ID so version 2.0 can upgrade an installed PDA build.

## Integration state

The new handheld workflow currently uses local BFC demonstration state so the native UX, scanner, permissions and warehouse transitions can be tested safely without changing the live website database contract.

A live write-capable BFC connection should use a dedicated authenticated warehouse API rather than customer/public /API/v1 routes. That API can then supply paid-order queues, inventory, returns and exceptions and accept audited pick/pack/dispatch actions.
