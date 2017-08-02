require.config({
  baseUrl : "/assets/javascripts",
  fileExclusionRegExp : /^lib$/,
  modules : [
    { name : 'admin/authorities/gazetteers' },
    { name : 'admin/authorities/people' },
    { name : 'admin/authorities/periods' },
    { name : 'admin/datasets/annotations' },
    { name : 'admin/datasets/csv' },
    { name : 'admin/maintenance/app' },
    { name : 'ui/app' }
  ]
});
