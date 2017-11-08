require.config({
  baseUrl : "/assets/javascripts",
  fileExclusionRegExp : /^lib$/,
  paths: { d3: '../../../web-modules/main/webjars/' },
  modules : [
    { name : 'admin/analytics/app' },
    { name : 'admin/authorities/gazetteers' },
    { name : 'admin/authorities/people' },
    { name : 'admin/authorities/periods' },
    { name : 'admin/datasets/annotations' },
    { name : 'admin/datasets/csv' },
    { name : 'admin/maintenance/app' },
    { name : 'embed/embed' },
    { name : 'landing/anim'},
    { name : 'ui/app' },
    { name : 'validator/app' }
  ]
});
