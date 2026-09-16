package com.example.model

data class BlackpoolBar(
    val name: String,
    val address: String,
    val area: String,
    val barType: String,
    val contact: String = "01253 000000"
)

object BlackpoolVenues {
    val AREAS = listOf(
        "All Blackpool",
        "Queen St & Gay Village",
        "Talbot Rd & Town Centre",
        "Promenade & Piers",
        "South Shore & Waterloo"
    )

    val ALL_BARS: List<BlackpoolBar> = listOf(
        // Queen Street & Gay Village
        BlackpoolBar("The Flying Handbag", "Queen St, Blackpool FY1 2NL", "Queen St & Gay Village", "Cabaret & DJ Bar", "01253 624519"),
        BlackpoolBar("Kaos Bar", "Queen St, Blackpool FY1 2NL", "Queen St & Gay Village", "Nightclub & DJ Bar", "01253 291129"),
        BlackpoolBar("Funny Girls", "Dickson Rd, Blackpool FY1 2AP", "Queen St & Gay Village", "Iconic Burlesque & Cabaret", "01253 649194"),
        BlackpoolBar("Cahoots", "High St / Queen St, Blackpool FY1 2NL", "Queen St & Gay Village", "Party Bar & Dancefloor", "01253 624519"),
        BlackpoolBar("Peek-a-booze", "Dickson Rd, Blackpool FY1 2AX", "Queen St & Gay Village", "Cabaret Bar & Hotel", "01253 623126"),
        BlackpoolBar("The Flamingo Club", "Queen St, Blackpool FY1 2NL", "Queen St & Gay Village", "Late Night Dance Club", "01253 624918"),
        BlackpoolBar("Mardi Gras", "Talbot Rd, Blackpool FY1 1LL", "Queen St & Gay Village", "Showbar & Entertainment", "01253 628990"),
        BlackpoolBar("The Duke of York", "Dickson Rd, Blackpool FY1 2AP", "Queen St & Gay Village", "Community & DJ Pub", "01253 624103"),
        BlackpoolBar("The Rose & Crown", "Corporation St, Blackpool FY1 1EJ", "Queen St & Gay Village", "Traditional Bar & DJs", "01253 621183"),

        // Talbot Road & Town Centre
        BlackpoolBar("Trilogy Nightclub", "11-15 Talbot Rd, Blackpool FY1 1LB", "Talbot Rd & Town Centre", "Superclub (3 Arena Rooms)", "01253 293373"),
        BlackpoolBar("Home & HQ Nightclub", "Talbot Rd, Blackpool FY1 1LB", "Talbot Rd & Town Centre", "Dual Arena Club & Lounge", "01253 751101"),
        BlackpoolBar("Walkabout Blackpool", "1-9 Queen St, Blackpool FY1 1NL", "Talbot Rd & Town Centre", "Sports, Live Acts & DJ Bar", "01253 749132"),
        BlackpoolBar("The Galleon Bar", "68 Abingdon St, Blackpool FY1 1DA", "Talbot Rd & Town Centre", "Late Night Live Music & DJ Legend", "01253 628994"),
        BlackpoolBar("Bootleg Social", "30 Topping St, Blackpool FY1 3AQ", "Talbot Rd & Town Centre", "Indie Live Music & Underground DJ Venue", "01253 932467"),
        BlackpoolBar("Dirty Blondes", "3 Back Church St, Blackpool FY1 1HP", "Talbot Rd & Town Centre", "Rock & DJ Cocktail Dive Bar", "01253 292888"),
        BlackpoolBar("Common Bar & Kitchen", "Edward St, Blackpool FY1 1BA", "Talbot Rd & Town Centre", "Cocktail Lounge & Beats", "01253 290001"),
        BlackpoolBar("The Counting House", "Talbot Square, Blackpool FY1 1NZ", "Talbot Rd & Town Centre", "City Pub & Weekend DJs", "01253 752071"),
        BlackpoolBar("Scrooges", "Corporation St, Blackpool FY1 1EJ", "Talbot Rd & Town Centre", "Retro Party Bar & Dancefloor", "01253 621009"),
        BlackpoolBar("The Mitre", "West St, Blackpool FY1 1HA", "Talbot Rd & Town Centre", "Town Centre Sports & Music", "01253 622340"),
        BlackpoolBar("The Albert and The Lion", "Bank Hey St / Promenade, Blackpool FY1 4TU", "Talbot Rd & Town Centre", "Promenade Bar (Wetherspoons)", "01253 299040"),
        BlackpoolBar("The Layton Rakes", "Market St, Blackpool FY1 1EX", "Talbot Rd & Town Centre", "Central Social Hub & Music", "01253 297740"),
        BlackpoolBar("Yates Blackpool", "Market St & Promenade, Blackpool FY1 1ET", "Talbot Rd & Town Centre", "High Energy DJ & Party Bar", "01253 623813"),
        BlackpoolBar("Shenanigans Irish Bar", "Clifton St, Blackpool FY1 1JP", "Talbot Rd & Town Centre", "Irish Music & DJ Party Bar", "01253 622709"),

        // Promenade, Piers & The Tower
        BlackpoolBar("Popworld Blackpool", "Promenade, Blackpool FY1 1NW", "Promenade & Piers", "Pop & Retro Party Club", "01253 294242"),
        BlackpoolBar("Viva Blackpool", "3 Church St, Promenade, Blackpool FY1 1HJ", "Promenade & Piers", "Vegas Style Entertainment & Gala Venue", "01253 297297"),
        BlackpoolBar("The Blackpool Tower Ballroom", "Promenade, Blackpool FY1 4BJ", "Promenade & Piers", "World Famous Historic Ballroom", "01253 622242"),
        BlackpoolBar("The Blackpool Tower Circus & Arena", "Promenade, Blackpool FY1 4BJ", "Promenade & Piers", "Arena Showcase Venue", "01253 622242"),
        BlackpoolBar("The Manchester Bar", "98-100 Promenade, Blackpool FY1 5AA", "Promenade & Piers", "Promenade Music & Party Bar", "01253 627196"),
        BlackpoolBar("North Pier Sun Lounge & Theatre", "North Pier, Promenade, Blackpool FY1 1NE", "Promenade & Piers", "Historic Pier Showbar & Deck", "01253 623304"),
        BlackpoolBar("Central Pier Showbar & Pirate's Bay", "Central Pier, Promenade, Blackpool FY1 5BB", "Promenade & Piers", "Showbar & Night Venue", "01253 622231"),
        BlackpoolBar("South Pier Laughing Donkey", "South Pier, Promenade, Blackpool FY4 1BB", "Promenade & Piers", "South Pier Sports & Live Music Bar", "01253 341030"),
        BlackpoolBar("The Dutton Arms", "441 Promenade, Blackpool FY4 1AR", "Promenade & Piers", "South Promenade Seaside Pub", "01253 342266"),
        BlackpoolBar("Soul Suite", "Promenade, Blackpool FY1 5AA", "Promenade & Piers", "Soul & Motown DJ Club", "01253 291129"),
        BlackpoolBar("The Pump & Truncheon", "Bonny St, Blackpool FY1 5AR", "Promenade & Piers", "Real Ale & Underground Music Pub", "01253 627252"),
        BlackpoolBar("Sands Venue Resort", "Promenade, Blackpool FY1 4TQ", "Promenade & Piers", "Luxury Cabaret & Event Lounge", "01253 625262"),
        BlackpoolBar("Winter Gardens & Empress Ballroom", "97 Church St, Blackpool FY1 1HL", "Promenade & Piers", "Grand Festival & Concert Ballroom", "01253 625252"),
        BlackpoolBar("Uncle Tom's Cabin", "Queens Promenade, Blackpool FY2 9HD", "Promenade & Piers", "North Shore Historic Pub & Terrace", "01253 591321"),
        BlackpoolBar("The Jaggy Thistle", "Foxhall Rd, Blackpool FY1 5BL", "Promenade & Piers", "Scottish & Party Bar", "01253 624505"),

        // South Shore & Waterloo Road
        BlackpoolBar("The Waterloo Music Bar", "Waterloo Rd, Blackpool FY4 2AF", "South Shore & Waterloo", "Legendary Rock & Live Music Venue", "01253 407886"),
        BlackpoolBar("Ma Kelly's Central", "Bank Hey St, Blackpool FY1 4RP", "Talbot Rd & Town Centre", "Famous Blackpool Entertainment Bar", "01253 298198"),
        BlackpoolBar("Ma Kelly's Showboat", "Promenade, Blackpool FY1 5AA", "Promenade & Piers", "Showbar & Vocalist Stage", "01253 298198"),
        BlackpoolBar("Ma Kelly's North", "Talbot Rd, Blackpool FY1 1LF", "Talbot Rd & Town Centre", "Live Music & Karaoke Bar", "01253 298198"),
        BlackpoolBar("Ma Kelly's South", "Lytham Rd, Blackpool FY4 1HT", "South Shore & Waterloo", "South Shore Cabaret Bar", "01253 298198"),
        BlackpoolBar("The Velvet Coaster", "49-51 New South Promenade, Blackpool FY4 1NF", "South Shore & Waterloo", "Massive Multi-floor Seafront Venue", "01253 341410"),
        BlackpoolBar("The Highfield", "Highfield Rd, Blackpool FY4 2JF", "South Shore & Waterloo", "South Shore Live Pub & DJ", "01253 401672"),
        BlackpoolBar("The Saddle Inn", "Whitegate Dr, Blackpool FY3 9JL", "Talbot Rd & Town Centre", "Whitegate Drive Community Music Bar", "01253 762294"),
        BlackpoolBar("The No. 3 Social Club", "Subway Rd, Blackpool FY3 8AA", "Talbot Rd & Town Centre", "Live Entertainment & Event Stage", "01253 392230")
    )
}
