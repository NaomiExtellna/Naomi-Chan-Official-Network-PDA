package com.example.model

data class BlackpoolBar(
    val name: String,
    val address: String,
    val area: String,
    val barType: String,
    val contact: String = "",
    val nearestTramStop: String = "",
    val notes: String = ""
)

object BlackpoolVenues {
    val ALL_BARS: List<BlackpoolBar> = listOf(
        // Queen Street & Gay Village
        BlackpoolBar("The Flying Handbag", "Queen St, Blackpool FY1 2NL", "Queen St & Gay Village", "Cabaret & DJ Bar", "01253 624519", "North Pier", "Late-night cabaret, DJs and entertainment."),
        BlackpoolBar("Kaos Bar", "Queen St, Blackpool FY1 2NL", "Queen St & Gay Village", "Nightclub & DJ Bar", "01253 291129", "North Pier"),
        BlackpoolBar("Funny Girls", "Dickson Rd, Blackpool FY1 2AP", "Queen St & Gay Village", "Cabaret Theatre & Showbar", "01253 649194", "North Pier", "Large cabaret venue with staged evening shows."),
        BlackpoolBar("Cahoots", "High St / Queen St, Blackpool FY1 2NL", "Queen St & Gay Village", "Party Bar & Dancefloor", "01253 624519", "North Pier"),
        BlackpoolBar("Peek-a-booze", "Dickson Rd, Blackpool FY1 2AX", "Queen St & Gay Village", "Cabaret Bar & Hotel", "01253 623126", "North Pier"),
        BlackpoolBar("The Flamingo Club", "Queen St, Blackpool FY1 2NL", "Queen St & Gay Village", "Late Night Dance Club", "01253 624918", "North Pier"),
        BlackpoolBar("Mardi Gras", "Talbot Rd, Blackpool FY1 1LL", "Queen St & Gay Village", "Showbar & Entertainment", "01253 628990", "North Pier"),
        BlackpoolBar("The Duke of York", "Dickson Rd, Blackpool FY1 2AP", "Queen St & Gay Village", "Community & DJ Pub", "01253 624103", "North Pier"),
        BlackpoolBar("The Rose & Crown", "Corporation St, Blackpool FY1 1EJ", "Queen St & Gay Village", "Traditional Bar & DJs", "01253 621183", "North Pier"),

        // Talbot Road & Town Centre
        BlackpoolBar("Trilogy Nightclub", "11-15 Talbot Rd, Blackpool FY1 1LB", "Talbot Rd & Town Centre", "Superclub / Multi-room Nightclub", "01253 293373", "North Pier"),
        BlackpoolBar("Home & HQ Nightclub", "Talbot Rd, Blackpool FY1 1LB", "Talbot Rd & Town Centre", "Dual Arena Club & Lounge", "01253 751101", "North Pier"),
        BlackpoolBar("Walkabout Blackpool", "1-9 Queen St, Blackpool FY1 1NL", "Talbot Rd & Town Centre", "Sports, Live Acts & DJ Bar", "01253 749132", "North Pier"),
        BlackpoolBar("The Galleon Bar", "68 Abingdon St, Blackpool FY1 1DA", "Talbot Rd & Town Centre", "Late Night Live Music & DJ Venue", "01253 628994", "North Pier"),
        BlackpoolBar("Bootleg Social", "30 Topping St, Blackpool FY1 3AQ", "Talbot Rd & Town Centre", "Indie Live Music & Underground DJ Venue", "01253 932467", "North Pier"),
        BlackpoolBar("Dirty Blondes", "3 Back Church St, Blackpool FY1 1HP", "Talbot Rd & Town Centre", "Rock & DJ Cocktail Bar", "01253 292888", "Tower"),
        BlackpoolBar("Common Bar & Kitchen", "Edward St, Blackpool FY1 1BA", "Talbot Rd & Town Centre", "Cocktail Lounge & Music", "01253 290001", "North Pier"),
        BlackpoolBar("The Counting House", "Talbot Square, Blackpool FY1 1NZ", "Talbot Rd & Town Centre", "City Pub & Weekend DJs", "01253 752071", "North Pier"),
        BlackpoolBar("Scrooges", "Corporation St, Blackpool FY1 1EJ", "Talbot Rd & Town Centre", "Retro Party Bar & Dancefloor", "01253 621009", "North Pier"),
        BlackpoolBar("The Mitre", "West St, Blackpool FY1 1HA", "Talbot Rd & Town Centre", "Town Centre Sports & Music", "01253 622340", "Tower"),
        BlackpoolBar("The Albert and The Lion", "Bank Hey St / Promenade, Blackpool FY1 4TU", "Talbot Rd & Town Centre", "Promenade Pub", "01253 299040", "Tower"),
        BlackpoolBar("The Layton Rakes", "Market St, Blackpool FY1 1EX", "Talbot Rd & Town Centre", "Town Centre Pub & Music", "01253 297740", "Tower"),
        BlackpoolBar("Yates Blackpool", "Market St & Promenade, Blackpool FY1 1ET", "Talbot Rd & Town Centre", "DJ & Party Bar", "01253 623813", "Tower"),
        BlackpoolBar("Shenanigans Irish Bar", "Clifton St, Blackpool FY1 1JP", "Talbot Rd & Town Centre", "Irish Music & Party Bar", "01253 622709", "North Pier"),
        BlackpoolBar("Ma Kelly's Central", "Bank Hey St, Blackpool FY1 4RP", "Talbot Rd & Town Centre", "Entertainment Bar", "01253 298198", "Tower"),
        BlackpoolBar("Ma Kelly's North", "Talbot Rd, Blackpool FY1 1LF", "Talbot Rd & Town Centre", "Live Music & Karaoke Bar", "01253 298198", "North Pier"),
        BlackpoolBar("The Saddle Inn", "Whitegate Dr, Blackpool FY3 9JL", "Talbot Rd & Town Centre", "Community Music Pub", "01253 762294"),
        BlackpoolBar("The No. 3 Social Club", "Subway Rd, Blackpool FY3 8AA", "Talbot Rd & Town Centre", "Live Entertainment & Event Stage", "01253 392230"),

        // Promenade, Piers & Tower area
        BlackpoolBar("Popworld Blackpool", "Promenade, Blackpool FY1 1NW", "Promenade & Piers", "Pop & Retro Party Club", "01253 294242", "North Pier"),
        BlackpoolBar("Viva Blackpool", "3 Church St, Blackpool FY1 1HJ", "Promenade & Piers", "Show, Cabaret & Event Venue", "01253 297297", "Tower"),
        BlackpoolBar("The Blackpool Tower Ballroom", "Promenade, Blackpool FY1 4BJ", "Promenade & Piers", "Historic Ballroom & Major Event Venue", "01253 622242", "Tower", "Ballroom events, dancing, private functions and special productions."),
        BlackpoolBar("The Blackpool Tower Circus & Arena", "Promenade, Blackpool FY1 4BJ", "Promenade & Piers", "Circus & Arena Venue", "01253 622242", "Tower"),
        BlackpoolBar("The Manchester Bar", "98-100 Promenade, Blackpool FY1 5AA", "Promenade & Piers", "Promenade Music & Party Bar", "01253 627196", "Central Pier"),
        BlackpoolBar("North Pier Sun Lounge & Theatre", "North Pier, Promenade, Blackpool FY1 1NE", "Promenade & Piers", "Historic Pier Showbar & Deck", "01253 623304", "North Pier"),
        BlackpoolBar("Joe Longthorne Theatre", "North Pier, Promenade, Blackpool FY1 1NE", "Promenade & Piers", "Theatre & Live Entertainment", "", "North Pier"),
        BlackpoolBar("Central Pier Showbar & Pirate's Bay", "Central Pier, Promenade, Blackpool FY1 5BB", "Promenade & Piers", "Showbar & Night Venue", "01253 622231", "Central Pier"),
        BlackpoolBar("South Pier Laughing Donkey", "South Pier, Promenade, Blackpool FY4 1BB", "Promenade & Piers", "Live Music & Entertainment Bar", "01253 341030", "South Pier"),
        BlackpoolBar("The Dutton Arms", "441 Promenade, Blackpool FY4 1AR", "Promenade & Piers", "South Promenade Pub", "01253 342266", "South Pier"),
        BlackpoolBar("Soul Suite", "Promenade, Blackpool FY1 5AA", "Promenade & Piers", "Soul & Motown DJ Club", "01253 291129", "Central Pier"),
        BlackpoolBar("The Pump & Truncheon", "Bonny St, Blackpool FY1 5AR", "Promenade & Piers", "Pub & Music Venue", "01253 627252", "Central Pier"),
        BlackpoolBar("Sands Venue Resort", "Promenade, Blackpool FY1 4TQ", "Promenade & Piers", "Cabaret & Event Lounge", "01253 625262", "Tower"),
        BlackpoolBar("Ma Kelly's Showboat", "Promenade, Blackpool FY1 5AA", "Promenade & Piers", "Showbar & Vocalist Stage", "01253 298198", "Central Pier"),
        BlackpoolBar("Uncle Tom's Cabin", "Queens Promenade, Blackpool FY2 9HD", "Promenade & Piers", "North Shore Pub & Terrace", "01253 591321", "Bispham"),
        BlackpoolBar("The Jaggy Thistle", "Foxhall Rd, Blackpool FY1 5BL", "Promenade & Piers", "Scottish & Party Bar", "01253 624505", "Central Pier"),

        // Winter Gardens, theatres & civic event venues
        BlackpoolBar("Winter Gardens Blackpool", "97 Church St, Blackpool FY1 1HL", "Winter Gardens & Theatre District", "Major Entertainment Complex", "01253 625252", "Tower", "Large complex containing the Opera House, Empress Ballroom, Spanish Hall, Olympia and conference spaces."),
        BlackpoolBar("Opera House - Winter Gardens", "97 Church St, Blackpool FY1 1HL", "Winter Gardens & Theatre District", "Theatre & Concert Venue", "01253 625252", "Tower"),
        BlackpoolBar("Empress Ballroom - Winter Gardens", "97 Church St, Blackpool FY1 1HL", "Winter Gardens & Theatre District", "Ballroom, Concert & Festival Venue", "01253 625252", "Tower"),
        BlackpoolBar("Spanish Hall - Winter Gardens", "97 Church St, Blackpool FY1 1HL", "Winter Gardens & Theatre District", "Functions, Conferences & Events", "01253 625252", "Tower"),
        BlackpoolBar("Olympia - Winter Gardens", "97 Church St, Blackpool FY1 1HL", "Winter Gardens & Theatre District", "Exhibition & Event Hall", "01253 625252", "Tower"),
        BlackpoolBar("Blackpool Conference & Exhibition Centre", "Winter Gardens, 97 Church St, Blackpool FY1 1HL", "Winter Gardens & Theatre District", "Conference & Exhibition Venue", "01253 625252", "Tower"),
        BlackpoolBar("Blackpool Grand Theatre", "33 Church St, Blackpool FY1 1HT", "Winter Gardens & Theatre District", "Historic Theatre", "01253 290190", "Tower", "Frank Matcham theatre hosting touring productions, comedy, dance and live performance."),
        BlackpoolBar("Tower Festival Headland", "Promenade, Blackpool FY1 4BJ", "Winter Gardens & Theatre District", "Outdoor Festival & Civic Event Space", "", "Tower", "Large seafront public event space opposite Blackpool Tower."),
        BlackpoolBar("Comedy Carpet", "Tower Festival Headland, Promenade, Blackpool FY1 4BJ", "Winter Gardens & Theatre District", "Outdoor Cultural Event Space", "", "Tower"),

        // South Shore, Pleasure Beach & Waterloo
        BlackpoolBar("The Waterloo Music Bar", "Waterloo Rd, Blackpool FY4 2AF", "South Shore & Waterloo", "Rock & Live Music Venue", "01253 407886", "Waterloo Road"),
        BlackpoolBar("Ma Kelly's South", "Lytham Rd, Blackpool FY4 1HT", "South Shore & Waterloo", "South Shore Cabaret Bar", "01253 298198", "South Pier"),
        BlackpoolBar("The Velvet Coaster", "49-51 New South Promenade, Blackpool FY4 1NF", "South Shore & Waterloo", "Multi-floor Seafront Venue", "01253 341410", "Pleasure Beach"),
        BlackpoolBar("The Highfield", "Highfield Rd, Blackpool FY4 2JF", "South Shore & Waterloo", "South Shore Live Pub & DJ", "01253 401672"),
        BlackpoolBar("Pleasure Beach Resort - The Globe", "Ocean Boulevard, Blackpool FY4 1EZ", "South Shore & Waterloo", "Theatre & Special Events Venue", "", "Pleasure Beach", "Indoor performance venue within Pleasure Beach Resort."),
        BlackpoolBar("Pleasure Beach Resort - Horseshoe", "Ocean Boulevard, Blackpool FY4 1EZ", "South Shore & Waterloo", "Showbar & Entertainment Venue", "", "Pleasure Beach"),
        BlackpoolBar("Pleasure Beach Arena", "Ocean Boulevard, Blackpool FY4 1EZ", "South Shore & Waterloo", "Ice Arena & Event Venue", "", "Pleasure Beach"),
        BlackpoolBar("Solaris Centre", "New South Promenade, Blackpool FY4 1RW", "South Shore & Waterloo", "Community, Wedding & Event Venue", "", "Harrow Place", "Seafront venue used for community events, private functions and exhibitions."),

        // Sports, hotels, conference & park venues
        BlackpoolBar("Bloomfield Road Stadium", "Seasiders Way, Blackpool FY1 6JJ", "Sports & Conference Venues", "Football Stadium & Hospitality Venue", "", "St Chads Road", "Home of Blackpool FC with hospitality and event facilities."),
        BlackpoolBar("Stanley Park Bandstand", "Stanley Park, West Park Dr, Blackpool FY3 9HQ", "Sports & Conference Venues", "Outdoor Music & Community Venue", "", "", "Seasonal performances and public events in Stanley Park."),
        BlackpoolBar("Stanley Park Pavilion", "Stanley Park, West Park Dr, Blackpool FY3 9HQ", "Sports & Conference Venues", "Park Pavilion & Event Space"),
        BlackpoolBar("Village Hotel Blackpool", "East Park Dr, Blackpool FY3 8LL", "Sports & Conference Venues", "Hotel, Conference & Event Venue", "", ""),
        BlackpoolBar("The Imperial Hotel Blackpool", "North Promenade, Blackpool FY1 2HB", "Sports & Conference Venues", "Hotel, Ballroom & Conference Venue", "", "Wilton Parade"),
        BlackpoolBar("Norbreck Castle Hotel", "Queens Promenade, Blackpool FY2 9AA", "Sports & Conference Venues", "Hotel, Exhibition & Conference Venue", "", "Norbreck"),
        BlackpoolBar("Blackpool Cricket Club", "Stanley Park, Blackpool FY3 9EQ", "Sports & Conference Venues", "Sports & Function Venue")
    )

    /** Alias used by the venue directory UI; kept alongside ALL_BARS for older code. */
    val EVENT_PLACES: List<BlackpoolBar> = ALL_BARS

    val AREAS: List<String> = listOf("All Blackpool") + EVENT_PLACES.map { it.area }.distinct()
}
