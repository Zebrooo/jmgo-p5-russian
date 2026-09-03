/*
 * FLauncher
 * Copyright (C) 2021  Étienne Fesser
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import 'package:flauncher/providers/apps_service.dart';
import 'package:flauncher/providers/settings_service.dart';
import 'package:flauncher/widgets/ensure_visible.dart';
import 'package:flauncher/widgets/settings/applications_panel_page.dart';
import 'package:flauncher/widgets/settings/categories_panel_page.dart';
import 'package:flauncher/widgets/settings/flauncher_about_dialog.dart';
import 'package:flauncher/widgets/settings/wallpaper_panel_page.dart';
import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:provider/provider.dart';

class SettingsPanelPage extends StatelessWidget {
  static const String routeName = "settings_panel";

  @override
  Widget build(BuildContext context) => Consumer<SettingsService>(
        builder: (context, settingsService, __) => SingleChildScrollView(
          child: Column(
            children: [
              Text("Настройки", style: Theme.of(context).textTheme.titleLarge),
              Divider(),
              EnsureVisible(
                alignment: 0.5,
                child: TextButton(
                  child: Row(
                    children: [
                      Icon(Icons.apps),
                      Container(width: 8),
                      Text("Приложения", style: Theme.of(context).textTheme.bodyMedium),
                    ],
                  ),
                  onPressed: () => Navigator.of(context).pushNamed(ApplicationsPanelPage.routeName),
                ),
              ),
              TextButton(
                child: Row(
                  children: [
                    Icon(Icons.category),
                    Container(width: 8),
                    Text("Категории", style: Theme.of(context).textTheme.bodyMedium),
                  ],
                ),
                onPressed: () => Navigator.of(context).pushNamed(CategoriesPanelPage.routeName),
              ),
              TextButton(
                child: Row(
                  children: [
                    Icon(Icons.wallpaper_outlined),
                    Container(width: 8),
                    Text("Обои", style: Theme.of(context).textTheme.bodyMedium),
                  ],
                ),
                onPressed: () => Navigator.of(context).pushNamed(WallpaperPanelPage.routeName),
              ),
              Divider(),
              TextButton(
                child: Row(
                  children: [
                    Icon(Icons.settings_outlined),
                    Container(width: 8),
                    Text("Настройки Android", style: Theme.of(context).textTheme.bodyMedium),
                  ],
                ),
                onPressed: () => context.read<AppsService>().openSettings(),
              ),
              Divider(),
              SwitchListTile(
                contentPadding: EdgeInsets.symmetric(horizontal: 8),
                value: settingsService.use24HourTimeFormat,
                onChanged: (value) => settingsService.setUse24HourTimeFormat(value),
                title: Text("24-часовой формат времени"),
                dense: true,
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.symmetric(horizontal: 8),
                value: settingsService.appHighlightAnimationEnabled,
                onChanged: (value) => settingsService.setAppHighlightAnimationEnabled(value),
                title: Text("Анимация выделения приложений"),
                dense: true,
              ),
              Divider(),
              SwitchListTile(
                contentPadding: EdgeInsets.symmetric(horizontal: 8),
                value: settingsService.crashReportsEnabled,
                onChanged: (value) => settingsService.setCrashReportsEnabled(value),
                title: Text("Отчёты об ошибках"),
                dense: true,
                subtitle: Text("Автоматически отправлять отчёты через Firebase Crashlytics."),
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.symmetric(horizontal: 8),
                value: settingsService.analyticsEnabled,
                onChanged: (value) => settingsService.setAnalyticsEnabled(value),
                title: Text("Аналитика"),
                dense: true,
                subtitle: Text("Отправлять обезличенные данные через Firebase Analytics."),
              ),
              Divider(),
              TextButton(
                child: Row(
                  children: [
                    Icon(Icons.info_outline),
                    Container(width: 8),
                    Text("О лаунчере", style: Theme.of(context).textTheme.bodyMedium),
                  ],
                ),
                onPressed: () => showDialog(
                  context: context,
                  builder: (_) => FutureBuilder<PackageInfo>(
                    future: PackageInfo.fromPlatform(),
                    builder: (context, snapshot) => snapshot.connectionState == ConnectionState.done
                        ? FLauncherAboutDialog(packageInfo: snapshot.data!)
                        : Container(),
                  ),
                ),
              ),
            ],
          ),
        ),
      );
}
