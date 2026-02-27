package com.openpaw.app.domain.tools;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class ToolRegistry_Factory implements Factory<ToolRegistry> {
  private final Provider<WhatsAppTool> whatsAppToolProvider;

  private final Provider<CalendarTool> calendarToolProvider;

  private final Provider<AlarmTool> alarmToolProvider;

  private final Provider<OpenAppTool> openAppToolProvider;

  private final Provider<MemoryTool> memoryToolProvider;

  private final Provider<ScreenTool> screenToolProvider;

  private final Provider<FileManagerTool> fileManagerToolProvider;

  private final Provider<SmsTool> smsToolProvider;

  private final Provider<ClipboardTool> clipboardToolProvider;

  public ToolRegistry_Factory(Provider<WhatsAppTool> whatsAppToolProvider,
      Provider<CalendarTool> calendarToolProvider, Provider<AlarmTool> alarmToolProvider,
      Provider<OpenAppTool> openAppToolProvider, Provider<MemoryTool> memoryToolProvider,
      Provider<ScreenTool> screenToolProvider, Provider<FileManagerTool> fileManagerToolProvider,
      Provider<SmsTool> smsToolProvider, Provider<ClipboardTool> clipboardToolProvider) {
    this.whatsAppToolProvider = whatsAppToolProvider;
    this.calendarToolProvider = calendarToolProvider;
    this.alarmToolProvider = alarmToolProvider;
    this.openAppToolProvider = openAppToolProvider;
    this.memoryToolProvider = memoryToolProvider;
    this.screenToolProvider = screenToolProvider;
    this.fileManagerToolProvider = fileManagerToolProvider;
    this.smsToolProvider = smsToolProvider;
    this.clipboardToolProvider = clipboardToolProvider;
  }

  @Override
  public ToolRegistry get() {
    return newInstance(whatsAppToolProvider.get(), calendarToolProvider.get(), alarmToolProvider.get(), openAppToolProvider.get(), memoryToolProvider.get(), screenToolProvider.get(), fileManagerToolProvider.get(), smsToolProvider.get(), clipboardToolProvider.get());
  }

  public static ToolRegistry_Factory create(Provider<WhatsAppTool> whatsAppToolProvider,
      Provider<CalendarTool> calendarToolProvider, Provider<AlarmTool> alarmToolProvider,
      Provider<OpenAppTool> openAppToolProvider, Provider<MemoryTool> memoryToolProvider,
      Provider<ScreenTool> screenToolProvider, Provider<FileManagerTool> fileManagerToolProvider,
      Provider<SmsTool> smsToolProvider, Provider<ClipboardTool> clipboardToolProvider) {
    return new ToolRegistry_Factory(whatsAppToolProvider, calendarToolProvider, alarmToolProvider, openAppToolProvider, memoryToolProvider, screenToolProvider, fileManagerToolProvider, smsToolProvider, clipboardToolProvider);
  }

  public static ToolRegistry newInstance(WhatsAppTool whatsAppTool, CalendarTool calendarTool,
      AlarmTool alarmTool, OpenAppTool openAppTool, MemoryTool memoryTool, ScreenTool screenTool,
      FileManagerTool fileManagerTool, SmsTool smsTool, ClipboardTool clipboardTool) {
    return new ToolRegistry(whatsAppTool, calendarTool, alarmTool, openAppTool, memoryTool, screenTool, fileManagerTool, smsTool, clipboardTool);
  }
}
