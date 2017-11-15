# unitedairgraph

To load schema and data:

Run UnitedAirlinesFactory in Eclipse

usage: UnitedAirlinesFactory <janusgraph-config-file> <data-files-directory>

To query flights:

With JanusGraph server
1. Copy date-helper.groovy to janusgraph/scripts
2. Update janusgraph/conf/gremlin-server/gremlin-server.yaml to have
   scripts: [scripts/empty-sample.groovy,scripts/date-helper.groovy]}}
3. Start JanusGraph server
4. Start gremlin console and use remote console
   :remote connect tinkerpop.server conf/remote-objects.yaml
   :remote console

Without JanusGraph server
1. Copy date-helper.groovy to janusgraph/scripts
2. Start gremlin console
   :load scripts/date-helper.groovy
   graph = JanusGraphFactory.open('conf/janusgraph-cassandra-es.properties')
   g=graph.traversal()

Sample queries:

1. Select all direct flights from SEA to SFO on 8/18/2017, departing time between 6:00 and 10:00 am
g.V().has('StationCode','SEA').outE('routes').has('LegEffectiveDate',lte(createDate(2017,8,18,0,0,0))).has('LegDiscountinueDate',gte(createDate(2017,8,18,0,0,0))).has('AircraftSTD',between(createTime(6,0,0),createTime(10,0,0))).inV().has('StationCode','SFO').path()

2. Select all one-stop flights, with one hour connection time, from SFO to SDF on 8/18/2017, departing time between 6:00 and 10:00 am
g.withSack{[M:4472,C:getMaxConnectTime('US','US')]}{it.clone()}.V().has('StationCode','SFO').outE('routes').has('LegEffectiveDate',lte(createDate(2017,8,18,0,0,0))).has('LegDiscountinueDate',gte(createDate(2017,8,18,0,0,0))).has('FlyFri',true).has('AircraftSTD',between(createTime(6,0,0),createTime(10,0,0))).has('ToAirport',without('SDF')).sack{m,e->m['M']=m['M']-e.value('FlightDistance');m['A1']=e.value('AircraftSTA');m}.filter{it.sack()['M']>0}.order().by('AircraftSTD',incr).inV().outE('routes').has('LegEffectiveDate',lte(createDate(2017,8,18,0,0,0))).has('LegDiscountinueDate',gte(createDate(2017,8,18,0,0,0))).has('FlyFri',true).has('ToAirport','SDF').sack{m,e->m['M']=m['M']-e.value('FlightDistance');m['C']=m['C']-getConnectTime(m['A1'],e.value('AircraftSTD'));m}.filter{it.sack()['M']>0 && it.sack()['C']>0 && it.get().value('AircraftSTD').after(addTime(it.sack()['A1'],0,60,0))}.inV().path().filter{passCabotage([it.get()[0].value('CountryCode'),it.get()[2].value('CountryCode'),it.get()[4].value('CountryCode')]) and passNextDay(it.get()[1].value('AircraftSTD'),it.get()[1].value('AircraftSTA'))}

3. Select all two-stop flights, with one hour connection time, from SFO to SDF on 8/18/2017, departing time between 6:00 and 10:00 am
g.withSack{[M:4472,C:getMaxConnectTime('US','US')]}{it.clone()}.V().has('StationCode','SFO').outE('routes').has('LegEffectiveDate',lte(createDate(2017,8,18,0,0,0))).has('LegDiscountinueDate',gte(createDate(2017,8,18,0,0,0))).has('FlyFri',true).has('AircraftSTD',between(createTime(6,0,0),createTime(10,0,0))).has('ToAirport',without('SDF')).sack{m,e->m['M']=m['M']-e.value('FlightDistance');m['A1']=e.value('AircraftSTA');m}.filter{it.sack()['M']>0}.order().by('AircraftSTD',incr).inV().outE('routes').has('LegEffectiveDate',lte(createDate(2017,8,18,0,0,0))).has('LegDiscountinueDate',gte(createDate(2017,8,18,0,0,0))).has('FlyFri',true).has('ToAirport',without('SDF','SFO')).sack{m,e->m['M']=m['M']-e.value('FlightDistance');m['C']=m['C']-getConnectTime(m['A1'],e.value('AircraftSTD'));m['A2']=e.value('AircraftSTA');m}.filter{it.sack()['M']>0 && it.sack()['C']>0 && it.get().value('AircraftSTD').after(addTime(it.sack()['A1'],0,60,0))}.inV().outE('routes').has('LegEffectiveDate',lte(createDate(2017,8,18,0,0,0))).has('LegDiscountinueDate',gte(createDate(2017,8,18,0,0,0))).has('FlyFri',true).has('ToAirport','SDF').sack{m,e->m['M']=m['M']-e.value('FlightDistance');m['C']=m['C']-getConnectTime(m['A2'],e.value('AircraftSTD'));m}.filter{it.sack()['M']>0 && it.sack()['C']>0 && it.get().value('AircraftSTD').after(addTime(it.sack()['A2'],0,60,0))}.inV().path().filter{passCabotage([it.get()[0].value('CountryCode'),it.get()[2].value('CountryCode'),it.get()[4].value('CountryCode'),it.get()[6].value('CountryCode')]) and passNextDay(it.get()[1].value('AircraftSTD'),it.get()[1].value('AircraftSTA'))}

