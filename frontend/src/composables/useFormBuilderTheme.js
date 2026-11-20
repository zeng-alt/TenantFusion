import { computed } from 'vue'
import { useAppStore } from '@/store'

export function useFormBuilderTheme() {
  const appStore = useAppStore()
  return computed(() => ({
    theme: appStore.isDark ? 'dark' : 'light',
    themeOverrides: appStore.naiveThemeOverrides,
  }))
}
