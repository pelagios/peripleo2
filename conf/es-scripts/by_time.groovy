f = doc['temporal_bounds.from']
t = doc['temporal_bounds.to']
buckets = []
if (!(f.empty || t.empty))
  for (i=f.date.year; i<t.date.year; i+= interval) { buckets.add(i) }
buckets;
