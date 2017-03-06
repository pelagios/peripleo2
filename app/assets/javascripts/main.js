require.config({
  baseUrl : "/assets/javascripts",
  fileExclusionRegExp : /^lib$/,
  modules : [
    { name : 'admin/datasets/annotations' },
    { name : 'admin/authorities/gazetteers' }
  ]
});
