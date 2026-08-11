const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');

module.exports = defineConfig([
  expoConfig,
  {
    ignores: ['dist/*'],
    settings: {
      'import/resolver': {
        node: {
          extensions: ['.native.tsx', '.web.tsx', '.tsx', '.ts', '.js', '.jsx'],
        },
      },
    },
  },
]);

