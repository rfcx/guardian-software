import tkinter as tk
from commands.registration_ui import RemoveRegistrationPage
from commands.software_ui import SoftwarePage
from commands.classifier_ui import RemoveClassifierPage
from commands.diagnostics_ui import Diagnostics
from commands.internal_storage_ui import RemoveStoragePage

class MenuPage(tk.Frame):
   def __init__(self, parent, controller):
      tk.Frame.__init__(self, parent)

      self.columnconfigure([0, 1, 2, 3, 4, 5], weight = 1)
      functionality_list = tk.Label(self, text = "functionalities", font = ("Courier", 14), anchor = 'center')
      functionality_list.grid(row = 0, column = 0, sticky="nsew", columnspan = 5)

      diagnostics_button = tk.Button(self, text = "Diagnotics", fg = 'black', bg = "green", width = 50, height = 2, anchor = 'center', command = lambda : controller.show_frame(Diagnostics))
      diagnostics_button.grid(row = 1, column = 0, sticky = "nsew", columnspan = 5)

      remove_register_button = tk.Button(self, text = "Remove Registration", fg = 'black', bg = "green", width = 50, height = 2, anchor = 'center', command = lambda : controller.show_frame(RemoveRegistrationPage))
      remove_register_button.grid(row = 2, column = 0, sticky = "nsew", columnspan = 5)

      downgrade_button = tk.Button(self, text = "Install Software", fg = 'black', bg = "green", width = 50, height = 2, anchor = 'center', command = lambda : controller.show_frame(SoftwarePage))
      downgrade_button.grid(row=3, column=0, sticky="nsew", columnspan = 5)

      remove_classifier_button = tk.Button(self, text = "Install Classifier", fg = 'black', bg = "green", width = 50, height = 2, anchor = 'center', command = lambda : controller.show_frame(RemoveClassifierPage))
      remove_classifier_button.grid(row = 4, column = 0, sticky = "nsew", columnspan = 5)

      remove_classifier_button = tk.Button(self, text = "Remove Internal Storage", fg = 'black', bg = "green", width = 50, height = 2, anchor = 'center', command = lambda : controller.show_frame(RemoveStoragePage))
      remove_classifier_button.grid(row = 5, column = 0, sticky = "nsew", columnspan = 5)