// SPDX-License-Identifier: AGPL-3.0-only
// App-only presentation, installed before profile content is painted when supported.
(() => {
  const install = () => {
    const root = document.documentElement;
    if (!root) return false;
    if (!document.getElementById('antintwrk-native-presentation')) {
      const style = document.createElement('style');
      style.id = 'antintwrk-native-presentation';
      style.textContent = '[data-hero-profile] .ant-profile-ecosystem { display:none!important; }';
      (document.head || root).append(style);
    }
    return true;
  };
  if (!install()) {
    const observer = new MutationObserver(() => {
      if (install()) observer.disconnect();
    });
    observer.observe(document, {childList:true,subtree:true});
  }
})();
