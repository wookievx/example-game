@import play.api.libs.json._
@import com.example.config.Entry
@import com.example.model.units.UnitObject
@import com.example.model.units.UnitObject._
@import com.example.model.units.UnitTraits
@import com.example.model.OwnerId
@import com.example.model._
@import boopickle.Default._

@import services.AssetResolver

@(id: OwnerId, gameId: OwnerId, width: Int, lanes: Int, units: Seq[UnitTraits], initialState: PlayerState, availableUnits: Seq[UnitObject], resolver: AssetResolver)

WarGame.main(@{JavaScript(Json.toJson(id).toString())}, @{JavaScript(Json.toJson(gameId).toString())}, @width, @lanes, {baseHealth: @{initialState.baseHealth}, gold: @{initialState.gold}} , @{Json.toJson(availableUnits.map(Pickle.intoBytes(_)).map(bbToArrayBytes)).toString()},@{
    val entries = units.map(tr => Entry(tr.name, routes.Assets.versioned(resolver.getPrefixedIconName(tr)).toString()))
    JavaScript(Json.toJson(entries).toString())
}, @{
    val entries = units.map(tr => Entry(tr.name, routes.Assets.versioned(resolver.getPrefixedUnitSymbol(tr)).toString()))
    JavaScript(Json.toJson(entries).toString())
})
